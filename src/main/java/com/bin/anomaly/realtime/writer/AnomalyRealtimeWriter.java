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
    private volatile long lastRestReconcileMs = 0L;
    private volatile long lastSeriesPublishMs = 0L;

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
     * tip/final/top: interval-ms마다.
     * tip series: 매 루프.
     * 확정 series: series-publish-interval-ms 또는 확정봉 dirty 시.
     * REST 보정: rest-reconcile-interval-ms (gap이면 즉시).
     */
    @Scheduled(fixedDelayString = "${anomaly.realtime.interval-ms:2000}")
    public void publishLoop() {
        if (!warmedUp.get()) return;

        if (!leader) {
            leader = snapshotStore.tryAcquireWriterLock();
            if (!leader) return;
        } else {
            snapshotStore.renewWriterLock();
        }

        String timeframe = scoreProps.getTimeframe();
        Duration barDuration = AnomalyScoreMath.parseTimeframeToDuration(timeframe, scoreProps.getBarDuration());
        long nowMs = System.currentTimeMillis();
        try {
            if (shouldReconcileRest(nowMs, barDuration)) {
                for (AssetRealtimeEngine engine : engines.values()) {
                    try {
                        reconcileFromRest(engine, timeframe, barDuration);
                    } catch (Exception e) {
                        log.debug("[anomaly-writer] rest reconcile failed {}: {}",
                                engine.binding().venueSymbol(), e.getMessage());
                    }
                }
                lastRestReconcileMs = nowMs;
            }

            OffsetDateTime sampleTs = OffsetDateTime.now(ZoneOffset.UTC);
            boolean seriesDue = (nowMs - lastSeriesPublishMs) >= Math.max(1000L, realtimeProps.getSeriesPublishIntervalMs());
            boolean anyConfirmedDirty = false;

            for (AssetRealtimeEngine engine : engines.values()) {
                if (!engine.ready()) continue;
                engine.sampleTip(sampleTs);
                publishFastSnapshots(engine, timeframe);
                if (engine.peekConfirmedDirty()) {
                    anyConfirmedDirty = true;
                }
            }

            if (seriesDue || anyConfirmedDirty) {
                for (AssetRealtimeEngine engine : engines.values()) {
                    if (!engine.ready()) continue;
                    engine.consumeConfirmedDirty();
                    publishConfirmedSeries(engine, timeframe);
                }
                lastSeriesPublishMs = nowMs;
            }

            publishTops(timeframe);
            snapshotStore.touchUpdatedAt();
            snapshotStore.markWarmingUp(false);
        } catch (Exception e) {
            log.warn("[anomaly-writer] publish loop error: {}", e.getMessage());
        }
    }

    private boolean shouldReconcileRest(long nowMs, Duration barDuration) {
        if (!realtimeProps.isUseWebSocket()) {
            return true;
        }
        long interval = Math.max(1000L, realtimeProps.getRestReconcileIntervalMs());
        if (nowMs - lastRestReconcileMs >= interval) {
            return true;
        }
        // 확정봉 gap이면 즉시
        for (AssetRealtimeEngine engine : engines.values()) {
            OffsetDateTime lastFinal = engine.lastFinalBarTs();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (lastFinal == null || lastFinal.plus(barDuration).plusSeconds(15).isBefore(now)) {
                return true;
            }
        }
        return false;
    }

    /**
     * REST로 확정봉 gap-fill + tip OHLC 갱신.
     */
    private void reconcileFromRest(AssetRealtimeEngine engine, String timeframe, Duration barDuration) {
        SymbolBinding binding = engine.binding();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime lastFinal = engine.lastFinalBarTs();

        boolean gap = lastFinal == null
                || lastFinal.plus(barDuration).plusSeconds(15).isBefore(now);
        if (gap) {
            OffsetDateTime from = lastFinal != null
                    ? lastFinal.plusSeconds(1)
                    : now.minus(barDuration.multipliedBy(100));
            OffsetDateTime minFrom = now.minusDays(2);
            if (from.isBefore(minFrom)) {
                from = minFrom;
            }
            List<MarketCandle> hist = marketDataClientRouter.fetchHistory(binding, timeframe, from, now);
            for (MarketCandle c : hist) {
                if (c == null) continue;
                boolean closed = !c.ts().plus(barDuration).isAfter(now);
                if (closed) {
                    engine.onCandle(new MarketCandle(
                            c.ts(), c.open(), c.high(), c.low(), c.close(), c.volume(), true
                    ));
                }
            }
        }

        List<MarketCandle> recent = marketDataClientRouter.fetchRecent(binding, timeframe, 3);
        for (MarketCandle c : recent) {
            if (c == null) continue;
            engine.onCandle(c);
        }
    }

    /** tip series + final (매 루프). */
    private void publishFastSnapshots(AssetRealtimeEngine engine, String timeframe) {
        SymbolBinding b = engine.binding();
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime tipFrom = to.minus(realtimeProps.getTipRetention());
        AnomalyScoreSeriesResponse tipSeries = engine.buildTipSeries(tipFrom, to);
        snapshotStore.putSeriesTip(b.venueId(), b.instrumentId(), timeframe, tipSeries);

        for (String mode : scoreModes()) {
            AnomalyScoreFinalResponse fin = engine.buildFinal(mode);
            if (fin != null) {
                snapshotStore.putFinal(b.venueId(), b.instrumentId(), timeframe, mode, fin);
            }
        }
    }

    /** 확정봉 series (느린 주기). */
    private void publishConfirmedSeries(AssetRealtimeEngine engine, String timeframe) {
        SymbolBinding b = engine.binding();
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = to.minusDays(realtimeProps.getSeriesRetentionDays());
        AnomalyScoreSeriesResponse series = engine.buildConfirmedSeries(from, to);
        snapshotStore.putSeries(b.venueId(), b.instrumentId(), timeframe, series);
    }

    /** 워밍업 직후 1회: 확정+tip+final. */
    private void publishAssetSnapshots(AssetRealtimeEngine engine, String timeframe) {
        publishConfirmedSeries(engine, timeframe);
        publishFastSnapshots(engine, timeframe);
        engine.consumeConfirmedDirty();
    }

    private List<String> scoreModes() {
        List<String> modes = realtimeProps.getScoreModes();
        if (modes == null || modes.isEmpty()) {
            return List.of("consensus");
        }
        return modes.stream()
                .filter(m -> m != null && !m.isBlank())
                .map(m -> m.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void publishTops(String timeframe) {
        int limit = realtimeProps.getTopSnapshotLimit();
        int deltaBars = realtimeProps.getDeltaBars();
        for (String mode : scoreModes()) {
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
