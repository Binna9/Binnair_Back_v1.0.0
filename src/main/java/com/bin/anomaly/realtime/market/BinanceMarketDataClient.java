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
import java.util.function.BiConsumer;

/**
 * Binance spot klines REST + combined stream WS.
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

    private volatile WebSocketClient wsClient;
    private final Map<String, SymbolBinding> streamToBinding = new ConcurrentHashMap<>();

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
        String symbol = toBinanceSymbol(binding.venueSymbol());
        String interval = toBinanceInterval(timeframe);
        String url = props.getBinanceRestBaseUrl()
                + "/api/v3/klines?symbol=" + urlEncode(symbol)
                + "&interval=" + urlEncode(interval)
                + "&limit=1";
        JsonNode arr = getJson(url);
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return null;
        }
        // REST kline의 마지막 봉은 진행 중일 수 있음 → isFinal=false로 tip 처리
        return parseKlineArray(arr.get(0), false);
    }

    @Override
    public void subscribeKlines(
            List<SymbolBinding> bindings,
            String timeframe,
            BiConsumer<SymbolBinding, MarketCandle> onCandle
    ) {
        unsubscribeAll();
        if (bindings == null || bindings.isEmpty()) {
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

        // Binance combined stream: 한 URL에 여러 스트림. 너무 길면 청크.
        int chunkSize = 50;
        // 단순화: 첫 청크만 연결 (maxAssets로 제한 권장). 필요 시 다중 연결 확장.
        List<String> first = streams.size() > chunkSize ? streams.subList(0, chunkSize) : streams;
        String joined = String.join("/", first);
        String wsUrl = props.getBinanceWsBaseUrl() + "?streams=" + joined;

        try {
            URI uri = URI.create(wsUrl);
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
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
                    log.warn("[binance-ws] closed code={} reason={}", code, reason);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("[binance-ws] error", ex);
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            log.error("[binance-ws] connect failed: {}", e.getMessage());
        }
    }

    @Override
    @PreDestroy
    public void unsubscribeAll() {
        WebSocketClient client = wsClient;
        wsClient = null;
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
        streamToBinding.clear();
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

    private MarketCandle parseKlineArray(JsonNode row, boolean forceFinal) {
        if (row == null || !row.isArray() || row.size() < 6) return null;
        long openTime = row.get(0).asLong();
        double open = row.get(1).asDouble();
        double high = row.get(2).asDouble();
        double low = row.get(3).asDouble();
        double close = row.get(4).asDouble();
        double volume = row.get(5).asDouble();
        return new MarketCandle(
                OffsetDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneOffset.UTC),
                open, high, low, close, volume,
                forceFinal
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
