package com.bin.anomaly.realtime.writer;

import com.bin.anomaly.realtime.config.AnomalyRealtimeProperties;
import com.bin.anomaly.realtime.engine.AssetRealtimeEngine;
import com.bin.anomaly.realtime.market.MarketDataClientRouter;
import com.bin.anomaly.realtime.model.MarketCandle;
import com.bin.anomaly.realtime.model.SymbolBinding;
import com.bin.anomaly.realtime.redis.AnomalyRedisSnapshotStore;
import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreFinalResponse;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.model.AnomalyScoreTopResponse;
import com.bin.anomaly.filter.repository.MarketSymbolDao;
import com.bin.anomaly.score.service.AnomalyScoreMath;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 원천 시세 → 증분 점수 계산 → Redis 스냅샷 Writer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "anomaly.realtime.enabled", havingValue = "true", matchIfMissing = true)
public class AnomalyRealtimeWriter {

    private final AnomalyRealtimeProperties realtimeProps;
    private final AnomalyScoreProperties scoreProps;
    private final MarketSymbolDao marketSymbolDao;
    private final MarketDataClientRouter marketDataClientRouter;
    private final AnomalyRedisSnapshotStore snapshotStore;

    private final Map<String, AssetRealtimeEngine> engines = new ConcurrentHashMap<>();
    private final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private final AtomicBoolean warming = new AtomicBoolean(false);
    private volatile boolean leader = false;

    @PostConstruct
    public void startWarmupAsync() {
        Thread t = new Thread(this::warmupSafe, "anomaly-realtime-warmup");
        t.setDaemon(true);
        t.start();
    }

    @PreDestroy
    public void shutdown() {
        marketDataClientRouter.unsubscribeAll();
    }

    private void warmupSafe() {
        if (!warming.compareAndSet(false, true)) return;
        try {
            snapshotStore.markWarmingUp(true);
            List<SymbolBinding> bindings = loadBindings();
            if (bindings.isEmpty()) {
                log.warn("[anomaly-writer] no active symbol bindings for venues={}", realtimeProps.getVenueCodes());
                snapshotStore.markWarmingUp(false);
                warmedUp.set(true);
                return;
            }

            String timeframe = scoreProps.getTimeframe();
            Duration barDuration = AnomalyScoreMath.parseTimeframeToDuration(timeframe, scoreProps.getBarDuration());
            int seriesMax = AssetRealtimeEngine.seriesMaxBarsFor(barDuration, realtimeProps.getSeriesRetentionDays());
            int historyMax = Math.max(realtimeProps.getDeltaBars() + 5, 32);
            Duration tipRetention = realtimeProps.getTipRetention();
            int tipMax = AssetRealtimeEngine.tipMaxSamplesFor(tipRetention, realtimeProps.getIntervalMs());

            OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime from = to.minusDays(realtimeProps.getWarmupHistoryDays());

            int ok = 0;
            for (SymbolBinding binding : bindings) {
                try {
                    AssetRealtimeEngine engine = new AssetRealtimeEngine(
                            binding,
                            timeframe,
                            scoreProps.getScoreVersion(),
                            scoreProps.getWarmupMinDays(),
                            scoreProps.getWarmupMinBars(),
                            scoreProps.getStdEps(),
                            seriesMax,
                            historyMax,
                            tipRetention,
                            tipMax
                    );
                    List<MarketCandle> history = marketDataClientRouter.fetchHistory(binding, timeframe, from, to);
                    // 히스토리는 확정봉으로 취급
                    List<MarketCandle> finals = history.stream()
                            .map(c -> new MarketCandle(c.ts(), c.open(), c.high(), c.low(), c.close(), c.volume(), true))
                            .toList();
                    engine.warmup(finals);
                    engines.put(binding.assetKey(), engine);
                    publishAssetSnapshots(engine, timeframe);
                    ok++;
                    log.info("[anomaly-writer] warmed {} ({}/{}) candles={}",
                            binding.venueSymbol(), ok, bindings.size(), finals.size());
                } catch (Exception e) {
                    log.warn("[anomaly-writer] warmup failed for {}: {}", binding.venueSymbol(), e.getMessage());
                }
            }

            if (realtimeProps.isUseWebSocket()) {
                marketDataClientRouter.subscribeKlines(bindings, timeframe, this::onLiveCandle);
            }

            publishTops(timeframe);
            snapshotStore.touchUpdatedAt();
            snapshotStore.markWarmingUp(false);
            warmedUp.set(true);
            log.info("[anomaly-writer] warmup complete engines={}", engines.size());
        } catch (Exception e) {
            log.error("[anomaly-writer] warmup failed", e);
            snapshotStore.markWarmingUp(false);
        } finally {
            warming.set(false);
        }
    }

    private List<SymbolBinding> loadBindings() {
        List<String> codes = realtimeProps.getVenueCodes() == null
                ? List.of()
                : realtimeProps.getVenueCodes().stream()
                .map(c -> c.toLowerCase(Locale.ROOT))
                .toList();
        List<SymbolBinding> all = marketSymbolDao.listActiveSymbolBindings(codes);
        int max = realtimeProps.getMaxAssets();
        if (max > 0 && all.size() > max) {
            return all.subList(0, max);
        }
        return all;
    }

    private void onLiveCandle(SymbolBinding binding, MarketCandle candle) {
        AssetRealtimeEngine engine = engines.get(binding.assetKey());
        if (engine == null) return;
        engine.onCandle(candle);
    }

    /**
     * 1초(설정)마다 Redis 스냅샷 갱신.
     */
    @Scheduled(fixedDelayString = "${anomaly.realtime.interval-ms:1000}")
    public void publishLoop() {
        if (!warmedUp.get()) return;

        // 리더 락 (멀티 인스턴스)
        if (!leader) {
            leader = snapshotStore.tryAcquireWriterLock();
            if (!leader) return;
        } else {
            snapshotStore.renewWriterLock();
        }

        String timeframe = scoreProps.getTimeframe();
        try {
            if (!realtimeProps.isUseWebSocket()) {
                for (AssetRealtimeEngine engine : engines.values()) {
                    try {
                        MarketCandle latest = marketDataClientRouter.fetchLatest(engine.binding(), timeframe);
                        if (latest != null) {
                            engine.onCandle(latest);
                        }
                    } catch (Exception e) {
                        log.debug("[anomaly-writer] latest poll failed {}: {}",
                                engine.binding().venueSymbol(), e.getMessage());
                    }
                }
            }

            OffsetDateTime sampleTs = OffsetDateTime.now(ZoneOffset.UTC);
            for (AssetRealtimeEngine engine : engines.values()) {
                if (!engine.ready()) continue;
                engine.sampleTip(sampleTs);
                publishAssetSnapshots(engine, timeframe);
            }
            publishTops(timeframe);
            snapshotStore.touchUpdatedAt();
            snapshotStore.markWarmingUp(false);
        } catch (Exception e) {
            log.warn("[anomaly-writer] publish loop error: {}", e.getMessage());
        }
    }

    private void publishAssetSnapshots(AssetRealtimeEngine engine, String timeframe) {
        SymbolBinding b = engine.binding();
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = to.minusDays(realtimeProps.getSeriesRetentionDays());
        AnomalyScoreSeriesResponse series = engine.buildSeries(from, to);
        snapshotStore.putSeries(b.venueId(), b.instrumentId(), timeframe, series);

        for (String mode : List.of("consensus", "max")) {
            AnomalyScoreFinalResponse fin = engine.buildFinal(mode);
            if (fin != null) {
                snapshotStore.putFinal(b.venueId(), b.instrumentId(), timeframe, mode, fin);
            }
        }
    }

    private void publishTops(String timeframe) {
        int limit = realtimeProps.getTopSnapshotLimit();
        int deltaBars = realtimeProps.getDeltaBars();
        for (String mode : List.of("consensus", "max")) {
            List<AssetRealtimeEngine.ScannerRow> rows = new ArrayList<>();
            for (AssetRealtimeEngine engine : engines.values()) {
                AssetRealtimeEngine.ScannerRow row = engine.toScannerRow(mode, deltaBars);
                if (row != null) rows.add(row);
            }
            snapshotStore.putTop("AGG", timeframe, mode, buildTop("AGG", timeframe, mode, limit, deltaBars, rows));
            snapshotStore.putTop("VOL", timeframe, mode, buildTop("VOL", timeframe, mode, limit, deltaBars, rows));
            snapshotStore.putTop("RNG", timeframe, mode, buildTop("RNG", timeframe, mode, limit, deltaBars, rows));
            snapshotStore.putTop("RET", timeframe, mode, buildTop("RET", timeframe, mode, limit, deltaBars, rows));
        }
    }

    private AnomalyScoreTopResponse buildTop(
            String tab,
            String timeframe,
            String mode,
            int limit,
            int deltaBars,
            List<AssetRealtimeEngine.ScannerRow> rows
    ) {
        Comparator<AssetRealtimeEngine.ScannerRow> cmp = switch (tab) {
            case "VOL" -> Comparator.comparing(
                    (AssetRealtimeEngine.ScannerRow r) -> abs(r.zVol()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
            case "RNG" -> Comparator.comparing(
                    (AssetRealtimeEngine.ScannerRow r) -> abs(r.zRng()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
            case "RET" -> Comparator.comparing(
                    (AssetRealtimeEngine.ScannerRow r) -> r.zRetAbs(),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
            default -> Comparator.comparing(
                    (AssetRealtimeEngine.ScannerRow r) -> r.finalScore(),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
        };

        List<AssetRealtimeEngine.ScannerRow> sorted = rows.stream().sorted(cmp).limit(limit).toList();
        List<AnomalyScoreTopResponse.Item> items = new ArrayList<>(sorted.size());
        int rank = 1;
        for (AssetRealtimeEngine.ScannerRow r : sorted) {
            Double metric = switch (tab) {
                case "VOL" -> r.zVol();
                case "RNG" -> r.zRng();
                case "RET" -> r.zRetAbs();
                default -> r.finalScore();
            };
            Double delta = switch (tab) {
                case "VOL" -> r.deltaVol();
                case "RNG" -> r.deltaRng();
                case "RET" -> r.deltaRet();
                default -> r.deltaFinal();
            };
            items.add(new AnomalyScoreTopResponse.Item(
                    rank++,
                    r.venueId(),
                    r.instrumentId(),
                    r.symbol(),
                    r.ts(),
                    r.finalLevel(),
                    r.finalScore(),
                    r.driver(),
                    metric,
                    delta,
                    "RET".equals(tab) ? r.direction() : null
            ));
        }
        OffsetDateTime ts = items.isEmpty() ? null : items.get(0).ts();
        return new AnomalyScoreTopResponse(timeframe, mode, tab, limit, deltaBars, ts, items);
    }

    private static Double abs(Double v) {
        return v == null ? null : Math.abs(v);
    }
}
