package com.bin.anomaly.realtime.engine;

import com.bin.anomaly.realtime.model.MarketCandle;
import com.bin.anomaly.realtime.model.SymbolBinding;
import com.bin.anomaly.score.model.AnomalyScoreFinalResponse;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.service.AnomalyScoreMath;
import com.bin.anomaly.score.service.RollingWindowStats;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 단일 자산의 롤링 z-score / series / final 상태.
 * <p>
 * 확정봉(isFinal): 롤링 윈도우에 commit 후 series에 5m 1포인트.<br>
 * tip(미확정): 윈도우에 add 하지 않고 임시 z/score 계산 → final/top 반영.<br>
 * tip 샘플: Writer 주기마다 샘플 시각(ts)으로 tipHistory에 append (openTime 덮어쓰기 금지).
 */
public class AssetRealtimeEngine {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final List<Integer> WINDOWS = List.of(30, 60, 90);

    private final SymbolBinding binding;
    private final String timeframe;
    private final String scoreVersion;
    private final int warmupMinDays;
    private final Integer warmupMinBars;
    private final double stdEps;
    private final int seriesMaxBars;
    private final int historyMaxBars;
    private final Duration tipRetention;
    private final int tipMaxSamples;

    private final Map<Integer, WindowBundle> windows = new HashMap<>();
    private final Deque<AnomalyScoreSeriesResponse.Point> series = new ArrayDeque<>();
    private final Deque<AnomalyScoreSeriesResponse.Point> tipHistory = new ArrayDeque<>();
    private final Deque<MetricSample> metricHistory = new ArrayDeque<>();

    private Double prevClose;
    /** 마지막 확정봉 openTime (UTC). REST gap-fill / 중복 commit 방지. */
    private OffsetDateTime lastFinalBarTs;
    private MarketCandle tipCandle;
    /** tip 임시 점수 (윈도우 commit 없음). tipCandle이 있을 때만 non-null. */
    private ScoreBundle tipSnapshot;
    private boolean ready;
    /** 확정봉 series Redis 재publish 필요. */
    private boolean confirmedDirty;

    public AssetRealtimeEngine(
            SymbolBinding binding,
            String timeframe,
            String scoreVersion,
            int warmupMinDays,
            Integer warmupMinBars,
            double stdEps,
            int seriesMaxBars,
            int historyMaxBars,
            Duration tipRetention,
            int tipMaxSamples
    ) {
        this.binding = binding;
        this.timeframe = timeframe;
        this.scoreVersion = scoreVersion;
        this.warmupMinDays = warmupMinDays;
        this.warmupMinBars = warmupMinBars;
        this.stdEps = stdEps;
        this.seriesMaxBars = Math.max(seriesMaxBars, 100);
        this.historyMaxBars = Math.max(historyMaxBars, 20);
        this.tipRetention = tipRetention == null || tipRetention.isZero() || tipRetention.isNegative()
                ? Duration.ofHours(1)
                : tipRetention;
        this.tipMaxSamples = Math.max(tipMaxSamples, 60);
        for (int w : WINDOWS) {
            windows.put(w, new WindowBundle(w));
        }
    }

    public SymbolBinding binding() {
        return binding;
    }

    public boolean ready() {
        return ready;
    }

    public synchronized OffsetDateTime lastFinalBarTs() {
        return lastFinalBarTs;
    }

    public synchronized boolean consumeConfirmedDirty() {
        boolean d = confirmedDirty;
        confirmedDirty = false;
        return d;
    }

    public synchronized boolean peekConfirmedDirty() {
        return confirmedDirty;
    }

    public void warmup(List<MarketCandle> candles) {
        for (MarketCandle c : candles) {
            if (c == null) continue;
            applyFinalBar(c);
        }
        ready = hasAnyScoredPoint();
    }

    public synchronized void onCandle(MarketCandle candle) {
        if (candle == null) return;
        if (candle.isFinal()) {
            applyFinalBar(candle);
            tipCandle = null;
            tipSnapshot = null;
        } else {
            tipCandle = candle;
            tipSnapshot = scoreAgainstBaseline(candle, false);
        }
        ready = hasAnyScoredPoint() || tipSnapshot != null;
    }

    /**
     * Writer publish 주기마다 호출. tip 현재 OHLC/점수를 샘플 시각(ts)으로 tipHistory에 append.
     * 캔들 openTime으로 덮어쓰지 않는다.
     */
    public synchronized void sampleTip(OffsetDateTime sampleTsUtc) {
        if (tipCandle == null || tipSnapshot == null || sampleTsUtc == null) return;

        AnomalyScoreSeriesResponse.Point tipPoint = new AnomalyScoreSeriesResponse.Point(
                toKst(sampleTsUtc),
                tipCandle.open(), tipCandle.high(), tipCandle.low(), tipCandle.close(), tipCandle.volume(),
                tipSnapshot.scores(), tipSnapshot.drivers(), tipSnapshot.zMap(),
                Boolean.TRUE
        );

        // 동일 샘플 시각(초 단위 중복)이면 최신으로 교체
        if (!tipHistory.isEmpty() && tipHistory.peekLast().ts().equals(tipPoint.ts())) {
            tipHistory.removeLast();
        }
        tipHistory.addLast(tipPoint);
        pruneTipHistory(sampleTsUtc);
    }

    private void pruneTipHistory(OffsetDateTime nowUtc) {
        Instant cutoff = nowUtc.toInstant().minus(tipRetention);
        while (!tipHistory.isEmpty()) {
            Instant ts = tipHistory.peekFirst().ts().toInstant();
            if (ts.isBefore(cutoff)) {
                tipHistory.removeFirst();
            } else {
                break;
            }
        }
        while (tipHistory.size() > tipMaxSamples) {
            tipHistory.removeFirst();
        }
    }

    /**
     * @param commit true면 롤링 윈도우에 add + last* 갱신 (확정봉). false면 tip 임시 계산만.
     */
    private ScoreBundle scoreAgainstBaseline(MarketCandle c, boolean commit) {
        OffsetDateTime ts = c.ts();
        Double ret = AnomalyScoreMath.calcLogReturn(prevClose, c.close());
        Double logVol = AnomalyScoreMath.calcLogVol(c.volume());
        Double rng = AnomalyScoreMath.calcRange(c.high(), c.low(), c.close());

        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, String> drivers = new LinkedHashMap<>();
        Map<String, AnomalyScoreSeriesResponse.Z> zMap = new LinkedHashMap<>();
        Map<Integer, WindowScore> byWindow = new HashMap<>();

        for (int w : WINDOWS) {
            WindowBundle wb = windows.get(w);
            wb.ret.evictOlderThan(ts);
            wb.vol.evictOlderThan(ts);
            wb.rng.evictOlderThan(ts);

            boolean warmupOk = AnomalyScoreMath.hasWarmup(wb.ret, ts, warmupMinDays, warmupMinBars)
                    && AnomalyScoreMath.hasWarmup(wb.vol, ts, warmupMinDays, warmupMinBars)
                    && AnomalyScoreMath.hasWarmup(wb.rng, ts, warmupMinDays, warmupMinBars);

            Double zRet = AnomalyScoreMath.zscore(wb.ret, ret, stdEps);
            Double zVol = AnomalyScoreMath.zscore(wb.vol, logVol, stdEps);
            Double zRng = AnomalyScoreMath.zscore(wb.rng, rng, stdEps);
            Double score = warmupOk ? AnomalyScoreMath.maxAbs(zRet, zVol, zRng) : null;

            String key = String.valueOf(w);
            scores.put(key, score);
            drivers.put(key, AnomalyScoreMath.driverOfLower(zRet, zVol, zRng));
            zMap.put(key, new AnomalyScoreSeriesResponse.Z(zRet, zVol, zRng));
            byWindow.put(w, new WindowScore(score, zRet, zVol, zRng, ts));

            if (commit) {
                wb.lastScore = score;
                wb.lastZRet = zRet;
                wb.lastZVol = zVol;
                wb.lastZRng = zRng;
                wb.lastTs = ts;
                wb.ret.add(ts, ret);
                wb.vol.add(ts, logVol);
                wb.rng.add(ts, rng);
            }
        }

        return new ScoreBundle(ts, scores, drivers, zMap, byWindow);
    }

    private void applyFinalBar(MarketCandle c) {
        OffsetDateTime ts = c.ts();
        if (lastFinalBarTs != null && !ts.isAfter(lastFinalBarTs)) {
            return;
        }

        ScoreBundle scored = scoreAgainstBaseline(c, true);
        prevClose = c.close();
        lastFinalBarTs = ts;

        if (scored.scores().values().stream().allMatch(v -> v == null)) {
            return;
        }

        AnomalyScoreSeriesResponse.Point point = new AnomalyScoreSeriesResponse.Point(
                toKst(scored.ts()),
                c.open(), c.high(), c.low(), c.close(), c.volume(),
                scored.scores(), scored.drivers(), scored.zMap(),
                Boolean.FALSE
        );
        series.addLast(point);
        while (series.size() > seriesMaxBars) {
            series.removeFirst();
        }
        confirmedDirty = true;

        AnomalyScoreMath.FinalAgg agg = AnomalyScoreMath.evaluateFinal(
                scored.scores().get("30"), scored.scores().get("60"), scored.scores().get("90"), "consensus"
        );
        WindowScore w90 = scored.byWindow().get(90);
        metricHistory.addLast(new MetricSample(
                scored.ts(),
                agg.finalScore(),
                firstNonNull(w90.zVol(), scored.byWindow().get(60).zVol(), scored.byWindow().get(30).zVol()),
                firstNonNull(w90.zRng(), scored.byWindow().get(60).zRng(), scored.byWindow().get(30).zRng()),
                abs(firstNonNull(w90.zRet(), scored.byWindow().get(60).zRet(), scored.byWindow().get(30).zRet())),
                firstNonNull(w90.zRet(), scored.byWindow().get(60).zRet(), scored.byWindow().get(30).zRet())
        ));
        while (metricHistory.size() > historyMaxBars) {
            metricHistory.removeFirst();
        }
    }

    private Double liveZVol() {
        if (tipSnapshot != null) {
            return firstNonNull(
                    tipSnapshot.byWindow().get(90).zVol(),
                    tipSnapshot.byWindow().get(60).zVol(),
                    tipSnapshot.byWindow().get(30).zVol()
            );
        }
        return firstNonNull(windows.get(90).lastZVol, windows.get(60).lastZVol, windows.get(30).lastZVol);
    }

    private Double liveZRng() {
        if (tipSnapshot != null) {
            return firstNonNull(
                    tipSnapshot.byWindow().get(90).zRng(),
                    tipSnapshot.byWindow().get(60).zRng(),
                    tipSnapshot.byWindow().get(30).zRng()
            );
        }
        return firstNonNull(windows.get(90).lastZRng, windows.get(60).lastZRng, windows.get(30).lastZRng);
    }

    private Double liveZRet() {
        if (tipSnapshot != null) {
            return firstNonNull(
                    tipSnapshot.byWindow().get(90).zRet(),
                    tipSnapshot.byWindow().get(60).zRet(),
                    tipSnapshot.byWindow().get(30).zRet()
            );
        }
        return firstNonNull(windows.get(90).lastZRet, windows.get(60).lastZRet, windows.get(30).lastZRet);
    }

    private static Double abs(Double v) {
        return v == null ? null : Math.abs(v);
    }

    private static Double firstNonNull(Double... xs) {
        for (Double x : xs) if (x != null) return x;
        return null;
    }

    private boolean hasAnyScoredPoint() {
        return !series.isEmpty();
    }

    public synchronized AnomalyScoreSeriesResponse buildSeries(
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        return buildSeries(fromInclusive, toInclusive, true, true);
    }

    /** 확정봉만 (느린 Redis 키). */
    public synchronized AnomalyScoreSeriesResponse buildConfirmedSeries(
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        return buildSeries(fromInclusive, toInclusive, true, false);
    }

    /** tip 히스토리만 (빠른 Redis 키). */
    public synchronized AnomalyScoreSeriesResponse buildTipSeries(
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        return buildSeries(fromInclusive, toInclusive, false, true);
    }

    private AnomalyScoreSeriesResponse buildSeries(
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive,
            boolean includeConfirmed,
            boolean includeTips
    ) {
        List<AnomalyScoreSeriesResponse.Point> confirmed = new ArrayList<>();
        if (includeConfirmed) {
            for (AnomalyScoreSeriesResponse.Point p : series) {
                if (inRange(p.ts(), fromInclusive, toInclusive)) {
                    confirmed.add(p);
                }
            }
        }

        List<AnomalyScoreSeriesResponse.Point> tips = new ArrayList<>();
        if (includeTips) {
            for (AnomalyScoreSeriesResponse.Point p : tipHistory) {
                if (inRange(p.ts(), fromInclusive, toInclusive)) {
                    tips.add(p);
                }
            }
        }

        List<AnomalyScoreSeriesResponse.Point> points = mergeByTs(confirmed, tips);

        OffsetDateTime latestTs = null;
        Map<String, Double> latestScores = new LinkedHashMap<>();
        Map<String, Double> maxScores = new LinkedHashMap<>();
        Map<String, OffsetDateTime> maxScoreTs = new LinkedHashMap<>();
        for (String k : List.of("30", "60", "90")) {
            latestScores.put(k, null);
            maxScores.put(k, null);
            maxScoreTs.put(k, null);
        }

        for (AnomalyScoreSeriesResponse.Point p : points) {
            latestTs = p.ts();
            latestScores.put("30", p.scores().get("30"));
            latestScores.put("60", p.scores().get("60"));
            latestScores.put("90", p.scores().get("90"));
            updateMax("30", p.scores().get("30"), p.ts(), maxScores, maxScoreTs);
            updateMax("60", p.scores().get("60"), p.ts(), maxScores, maxScoreTs);
            updateMax("90", p.scores().get("90"), p.ts(), maxScores, maxScoreTs);
        }

        OffsetDateTime from = fromInclusive != null ? toKst(fromInclusive) : null;
        OffsetDateTime to = toInclusive != null ? toKst(toInclusive) : null;
        OffsetDateTime serverTime = OffsetDateTime.now(ZoneOffset.UTC).atZoneSameInstant(KST).toOffsetDateTime();

        AnomalyScoreSeriesResponse.Meta meta = new AnomalyScoreSeriesResponse.Meta(
                binding.venueId(),
                binding.venueCode(),
                binding.instrumentId(),
                binding.instrumentSymbol(),
                binding.venueSymbol(),
                timeframe,
                scoreVersion,
                WINDOWS,
                from,
                to,
                serverTime,
                points.size()
        );
        AnomalyScoreSeriesResponse.Summary summary = new AnomalyScoreSeriesResponse.Summary(
                latestTs, latestScores, maxScores, maxScoreTs
        );
        return new AnomalyScoreSeriesResponse(meta, summary, points);
    }

    /**
     * 확정봉 + tip 히스토리 시간순 merge. 같은 ts면 tip이 이긴다.
     */
    private static List<AnomalyScoreSeriesResponse.Point> mergeByTs(
            List<AnomalyScoreSeriesResponse.Point> confirmed,
            List<AnomalyScoreSeriesResponse.Point> tips
    ) {
        List<AnomalyScoreSeriesResponse.Point> out = new ArrayList<>(confirmed.size() + tips.size());
        int i = 0;
        int j = 0;
        while (i < confirmed.size() && j < tips.size()) {
            Instant a = confirmed.get(i).ts().toInstant();
            Instant b = tips.get(j).ts().toInstant();
            int cmp = a.compareTo(b);
            if (cmp < 0) {
                out.add(confirmed.get(i++));
            } else if (cmp > 0) {
                out.add(tips.get(j++));
            } else {
                out.add(tips.get(j++));
                i++;
            }
        }
        while (i < confirmed.size()) out.add(confirmed.get(i++));
        while (j < tips.size()) out.add(tips.get(j++));
        return out;
    }

    private static boolean inRange(OffsetDateTime tsKst, OffsetDateTime fromInclusive, OffsetDateTime toInclusive) {
        OffsetDateTime tsUtc = tsKst.atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
        if (fromInclusive != null && tsUtc.isBefore(fromInclusive)) return false;
        if (toInclusive != null && tsUtc.isAfter(toInclusive)) return false;
        return true;
    }

    public synchronized AnomalyScoreFinalResponse buildFinal(String mode) {
        Double s30;
        Double s60;
        Double s90;
        OffsetDateTime ts;
        List<AnomalyScoreFinalResponse.Component> components = new ArrayList<>();

        if (tipSnapshot != null) {
            s30 = tipSnapshot.scores().get("30");
            s60 = tipSnapshot.scores().get("60");
            s90 = tipSnapshot.scores().get("90");
            // top/final 배지: 가능하면 최신 tip 샘플 시각
            ts = !tipHistory.isEmpty()
                    ? tipHistory.peekLast().ts().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime()
                    : tipSnapshot.ts();
            for (int w : WINDOWS) {
                WindowScore ws = tipSnapshot.byWindow().get(w);
                components.add(new AnomalyScoreFinalResponse.Component(
                        w,
                        ws.score(),
                        AnomalyScoreMath.driverOf(ws.zRet(), ws.zVol(), ws.zRng()),
                        ws.zRet(), ws.zVol(), ws.zRng()
                ));
            }
        } else {
            s30 = windows.get(30).lastScore;
            s60 = windows.get(60).lastScore;
            s90 = windows.get(90).lastScore;
            if (s30 == null && s60 == null && s90 == null) {
                return null;
            }
            ts = windows.get(90).lastTs != null ? windows.get(90).lastTs
                    : (windows.get(60).lastTs != null ? windows.get(60).lastTs : windows.get(30).lastTs);
            for (int w : WINDOWS) {
                WindowBundle wb = windows.get(w);
                components.add(new AnomalyScoreFinalResponse.Component(
                        w,
                        wb.lastScore,
                        AnomalyScoreMath.driverOf(wb.lastZRet, wb.lastZVol, wb.lastZRng),
                        wb.lastZRet, wb.lastZVol, wb.lastZRng
                ));
            }
        }

        if (s30 == null && s60 == null && s90 == null) {
            return null;
        }
        AnomalyScoreMath.FinalAgg agg = AnomalyScoreMath.evaluateFinal(s30, s60, s90, mode);
        return new AnomalyScoreFinalResponse(
                toKst(ts),
                agg.mode(),
                agg.finalScore(),
                agg.finalLevel(),
                agg.basis(),
                components
        );
    }

    public synchronized ScannerRow toScannerRow(String mode, int deltaBars) {
        AnomalyScoreFinalResponse fin = buildFinal(mode);
        if (fin == null || fin.finalScore() == null) return null;

        Double metricVol = liveZVol();
        Double metricRng = liveZRng();
        Double zRet = liveZRet();
        Double metricRetAbs = abs(zRet);

        Double prevFinal = metricAtDelta(deltaBars, MetricSample::finalScore);
        Double delta = (fin.finalScore() != null && prevFinal != null)
                ? fin.finalScore() - prevFinal
                : null;

        String direction = "FLAT";
        if (zRet != null) {
            if (zRet > 0.1) direction = "UP";
            else if (zRet < -0.1) direction = "DOWN";
        }

        Double deltaVol;
        Double deltaRng;
        Double deltaRet;
        if (tipSnapshot != null) {
            delta = subtract(fin.finalScore(), lastMetric(MetricSample::finalScore));
            deltaVol = subtract(metricVol, lastMetric(MetricSample::zVol));
            deltaRng = subtract(metricRng, lastMetric(MetricSample::zRng));
            deltaRet = subtract(metricRetAbs, lastMetric(MetricSample::zRetAbs));
        } else {
            deltaVol = metricDelta(deltaBars, MetricSample::zVol);
            deltaRng = metricDelta(deltaBars, MetricSample::zRng);
            deltaRet = metricDelta(deltaBars, MetricSample::zRetAbs);
        }

        return new ScannerRow(
                binding.venueId(),
                binding.instrumentId(),
                binding.instrumentSymbol() != null ? binding.instrumentSymbol() : binding.venueSymbol(),
                fin.ts(),
                fin.finalLevel(),
                fin.finalScore(),
                fin.components().isEmpty() ? "NONE" : fin.components().get(fin.components().size() - 1).driver(),
                metricVol,
                metricRng,
                metricRetAbs,
                zRet,
                delta,
                direction,
                deltaVol,
                deltaRng,
                deltaRet
        );
    }

    private static Double subtract(Double a, Double b) {
        if (a == null || b == null) return null;
        return a - b;
    }

    private Double lastMetric(java.util.function.Function<MetricSample, Double> getter) {
        if (metricHistory.isEmpty()) return null;
        return getter.apply(metricHistory.peekLast());
    }

    private Double metricAtDelta(int deltaBars, java.util.function.Function<MetricSample, Double> getter) {
        if (metricHistory.size() <= deltaBars) return null;
        MetricSample[] arr = metricHistory.toArray(new MetricSample[0]);
        int idx = arr.length - 1 - deltaBars;
        if (idx < 0) return null;
        return getter.apply(arr[idx]);
    }

    private Double metricDelta(int deltaBars, java.util.function.Function<MetricSample, Double> getter) {
        if (metricHistory.isEmpty()) return null;
        MetricSample[] arr = metricHistory.toArray(new MetricSample[0]);
        Double now = getter.apply(arr[arr.length - 1]);
        Double prev = metricAtDelta(deltaBars, getter);
        return subtract(now, prev);
    }

    private static void updateMax(
            String key,
            Double score,
            OffsetDateTime ts,
            Map<String, Double> maxScores,
            Map<String, OffsetDateTime> maxScoreTs
    ) {
        if (score == null) return;
        Double cur = maxScores.get(key);
        if (cur == null || score > cur) {
            maxScores.put(key, score);
            maxScoreTs.put(key, ts);
        }
    }

    private static OffsetDateTime toKst(OffsetDateTime ts) {
        if (ts == null) return null;
        return ts.atZoneSameInstant(KST).toOffsetDateTime();
    }

    public static int seriesMaxBarsFor(Duration barDuration, int retentionDays) {
        long bars = Math.max(1, barDuration.toMinutes() <= 0 ? 1 : (retentionDays * 24L * 60L) / barDuration.toMinutes());
        return (int) Math.min(bars + 10, 50_000);
    }

    public static int tipMaxSamplesFor(Duration tipRetention, long intervalMs) {
        long ms = tipRetention == null ? Duration.ofHours(1).toMillis() : tipRetention.toMillis();
        long step = Math.max(intervalMs, 1L);
        long n = (ms / step) + 50;
        return (int) Math.min(Math.max(n, 60), 10_000);
    }

    private static final class WindowBundle {
        private final RollingWindowStats ret;
        private final RollingWindowStats vol;
        private final RollingWindowStats rng;
        private Double lastScore;
        private Double lastZRet;
        private Double lastZVol;
        private Double lastZRng;
        private OffsetDateTime lastTs;

        private WindowBundle(int windowDays) {
            Duration d = Duration.ofDays(windowDays);
            this.ret = new RollingWindowStats(d);
            this.vol = new RollingWindowStats(d);
            this.rng = new RollingWindowStats(d);
        }
    }

    private record WindowScore(
            Double score,
            Double zRet,
            Double zVol,
            Double zRng,
            OffsetDateTime ts
    ) {}

    private record ScoreBundle(
            OffsetDateTime ts,
            Map<String, Double> scores,
            Map<String, String> drivers,
            Map<String, AnomalyScoreSeriesResponse.Z> zMap,
            Map<Integer, WindowScore> byWindow
    ) {}

    private record MetricSample(
            OffsetDateTime ts,
            Double finalScore,
            Double zVol,
            Double zRng,
            Double zRetAbs,
            Double zRet
    ) {}

    public record ScannerRow(
            long venueId,
            long instrumentId,
            String symbol,
            OffsetDateTime ts,
            String finalLevel,
            Double finalScore,
            String driver,
            Double zVol,
            Double zRng,
            Double zRetAbs,
            Double zRet,
            Double deltaFinal,
            String direction,
            Double deltaVol,
            Double deltaRng,
            Double deltaRet
    ) {}
}
