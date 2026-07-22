package com.bin.anomaly.realtime.market;

import com.bin.anomaly.realtime.config.AnomalyRealtimeProperties;
import com.bin.anomaly.realtime.model.MarketCandle;
import com.bin.anomaly.realtime.model.SymbolBinding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Binance spot klines REST + combined stream WS (자동 재연결).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceMarketDataClient implements MarketDataClient {

    private final AnomalyRealtimeProperties props;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ScheduledExecutorService reconnectExec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "binance-ws-reconnect");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocketClient wsClient;
    private final Map<String, SymbolBinding> streamToBinding = new ConcurrentHashMap<>();
    private final AtomicBoolean intentionalClose = new AtomicBoolean(false);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private volatile List<SymbolBinding> subscribedBindings = List.of();
    private volatile String subscribedTimeframe;
    private volatile BiConsumer<SymbolBinding, MarketCandle> subscribedCallback;

    @Override
    public boolean supports(String venueCode) {
        return venueCode != null && "binance".equalsIgnoreCase(venueCode.trim());
    }

    @Override
    public List<MarketCandle> fetchHistory(
            SymbolBinding binding,
            String timeframe,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        String symbol = toBinanceSymbol(binding.venueSymbol());
        String interval = toBinanceInterval(timeframe);
        long start = fromInclusive.toInstant().toEpochMilli();
        long end = toInclusive.toInstant().toEpochMilli();

        List<MarketCandle> all = new ArrayList<>();
        long cursor = start;
        while (cursor < end) {
            String url = props.getBinanceRestBaseUrl()
                    + "/api/v3/klines?symbol=" + urlEncode(symbol)
                    + "&interval=" + urlEncode(interval)
                    + "&startTime=" + cursor
                    + "&endTime=" + end
                    + "&limit=1000";
            JsonNode arr = getJson(url);
            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                break;
            }
            long lastOpen = cursor;
            for (JsonNode row : arr) {
                MarketCandle c = parseKlineArray(row, true);
                if (c != null) {
                    all.add(c);
                    lastOpen = c.ts().toInstant().toEpochMilli();
                }
            }
            long next = lastOpen + 1;
            if (next <= cursor || arr.size() < 1000) {
                break;
            }
            cursor = next;
            sleepQuietly(50);
        }
        all.sort(Comparator.comparing(MarketCandle::ts));
        return all;
    }

    @Override
    public MarketCandle fetchLatest(SymbolBinding binding, String timeframe) {
        List<MarketCandle> recent = fetchRecent(binding, timeframe, 1);
        return recent.isEmpty() ? null : recent.get(0);
    }

    @Override
    public List<MarketCandle> fetchRecent(SymbolBinding binding, String timeframe, int limit) {
        int lim = Math.max(1, Math.min(limit, 1000));
        String symbol = toBinanceSymbol(binding.venueSymbol());
        String interval = toBinanceInterval(timeframe);
        String url = props.getBinanceRestBaseUrl()
                + "/api/v3/klines?symbol=" + urlEncode(symbol)
                + "&interval=" + urlEncode(interval)
                + "&limit=" + lim;
        JsonNode arr = getJson(url);
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return List.of();
        }
        long nowMs = Instant.now().toEpochMilli();
        List<MarketCandle> out = new ArrayList<>(arr.size());
        for (JsonNode row : arr) {
            MarketCandle c = parseKlineArray(row, nowMs);
            if (c != null) out.add(c);
        }
        return out;
    }

    @Override
    public void subscribeKlines(
            List<SymbolBinding> bindings,
            String timeframe,
            BiConsumer<SymbolBinding, MarketCandle> onCandle
    ) {
        intentionalClose.set(true);
        closeWsQuietly();
        intentionalClose.set(false);

        subscribedBindings = bindings == null ? List.of() : List.copyOf(bindings);
        subscribedTimeframe = timeframe;
        subscribedCallback = onCandle;
        connectWs();
    }

    private void connectWs() {
        List<SymbolBinding> bindings = subscribedBindings;
        String timeframe = subscribedTimeframe;
        BiConsumer<SymbolBinding, MarketCandle> onCandle = subscribedCallback;
        if (bindings == null || bindings.isEmpty() || onCandle == null) {
            return;
        }

        streamToBinding.clear();
        List<String> streams = new ArrayList<>();
        String interval = toBinanceInterval(timeframe);
        for (SymbolBinding b : bindings) {
            if (!supports(b.venueCode())) continue;
            String symbol = toBinanceSymbol(b.venueSymbol()).toLowerCase(Locale.ROOT);
            String stream = symbol + "@kline_" + interval;
            streams.add(stream);
            streamToBinding.put(stream, b);
        }
        if (streams.isEmpty()) {
            return;
        }

        int chunkSize = 50;
        List<String> first = streams.size() > chunkSize ? streams.subList(0, chunkSize) : streams;
        String joined = String.join("/", first);
        String wsUrl = props.getBinanceWsBaseUrl() + "?streams=" + joined;

        try {
            URI uri = URI.create(wsUrl);
            WebSocketClient client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    reconnectScheduled.set(false);
                    log.info("[binance-ws] connected streams={}", first.size());
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JsonNode root = objectMapper.readTree(message);
                        JsonNode data = root.has("data") ? root.get("data") : root;
                        String stream = root.has("stream") ? root.get("stream").asText() : null;
                        SymbolBinding binding = stream != null ? streamToBinding.get(stream) : null;
                        if (binding == null && data.has("s")) {
                            String sym = data.get("s").asText("").toLowerCase(Locale.ROOT);
                            binding = streamToBinding.get(sym + "@kline_" + interval);
                        }
                        if (binding == null || !data.has("k")) return;
                        MarketCandle candle = parseKlineEvent(data.get("k"));
                        if (candle != null) {
                            onCandle.accept(binding, candle);
                        }
                    } catch (Exception e) {
                        log.warn("[binance-ws] parse failed: {}", e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("[binance-ws] closed code={} reason={} remote={}", code, reason, remote);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("[binance-ws] error: {}", ex.getMessage());
                }
            };
            wsClient = client;
            client.connect();
        } catch (Exception e) {
            log.error("[binance-ws] connect failed: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (intentionalClose.get()) return;
        if (subscribedCallback == null || subscribedBindings.isEmpty()) return;
        if (!reconnectScheduled.compareAndSet(false, true)) return;
        reconnectExec.schedule(() -> {
            reconnectScheduled.set(false);
            if (intentionalClose.get()) return;
            log.info("[binance-ws] reconnecting...");
            closeWsQuietly();
            connectWs();
        }, 3, TimeUnit.SECONDS);
    }

    @Override
    @PreDestroy
    public void unsubscribeAll() {
        intentionalClose.set(true);
        closeWsQuietly();
        streamToBinding.clear();
        subscribedBindings = List.of();
        subscribedCallback = null;
        subscribedTimeframe = null;
        reconnectExec.shutdownNow();
    }

    private void closeWsQuietly() {
        WebSocketClient client = wsClient;
        wsClient = null;
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }

    private JsonNode getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("[binance-rest] HTTP {} url={}", response.statusCode(), url);
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            log.warn("[binance-rest] request failed: {} url={}", e.getMessage(), url);
            return null;
        }
    }

    /** 워밍업용: 전부 확정으로 취급. */
    private MarketCandle parseKlineArray(JsonNode row, boolean forceFinal) {
        return parseKlineArray(row, forceFinal ? Long.MAX_VALUE : Instant.now().toEpochMilli());
    }

    /**
     * closeTime &lt; nowMs 이면 확정봉, 아니면 tip.
     */
    private MarketCandle parseKlineArray(JsonNode row, long nowMs) {
        if (row == null || !row.isArray() || row.size() < 6) return null;
        long openTime = row.get(0).asLong();
        double open = row.get(1).asDouble();
        double high = row.get(2).asDouble();
        double low = row.get(3).asDouble();
        double close = row.get(4).asDouble();
        double volume = row.get(5).asDouble();
        boolean isFinal;
        if (nowMs == Long.MAX_VALUE) {
            isFinal = true;
        } else if (row.size() > 6) {
            long closeTime = row.get(6).asLong();
            isFinal = closeTime < nowMs;
        } else {
            isFinal = false;
        }
        return new MarketCandle(
                OffsetDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneOffset.UTC),
                open, high, low, close, volume,
                isFinal
        );
    }

    private MarketCandle parseKlineEvent(JsonNode k) {
        long openTime = k.get("t").asLong();
        double open = k.get("o").asDouble();
        double high = k.get("h").asDouble();
        double low = k.get("l").asDouble();
        double close = k.get("c").asDouble();
        double volume = k.get("v").asDouble();
        boolean isFinal = k.has("x") && k.get("x").asBoolean(false);
        return new MarketCandle(
                OffsetDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneOffset.UTC),
                open, high, low, close, volume,
                isFinal
        );
    }

    public static String toBinanceSymbol(String venueSymbol) {
        if (venueSymbol == null) {
            throw new IllegalArgumentException("venueSymbol is null");
        }
        String s = venueSymbol.trim().toUpperCase(Locale.ROOT);
        s = s.replace("/", "").replace("-", "").replace("_", "").replace(":", "");
        return s;
    }

    public static String toBinanceInterval(String timeframe) {
        if (timeframe == null || timeframe.isBlank()) return "5m";
        return timeframe.trim().toLowerCase(Locale.ROOT);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
