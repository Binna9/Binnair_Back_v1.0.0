package com.bin.anomaly.score.service;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreDetectRequest;
import com.bin.anomaly.score.model.CandleRow;
import com.bin.anomaly.score.model.VenueInstrument;
import com.bin.anomaly.score.repository.AnomalyScoreUpsertDao;
import com.bin.anomaly.score.repository.CoreMarketDataDao;
import com.bin.anomaly.score.repository.PipelineRunDao;
import com.bin.anomaly.score.model.AnomalyScoreDetectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnomalyScoreDetectService {

    private final CoreMarketDataDao coreMarketDataDao;
    private final PipelineRunDao pipelineRunDao;
    private final AnomalyScoreUpsertDao anomalyScoreUpsertDao;
    private final AnomalyScoreProperties props;

    /**
     * Detect 잡(배치) 실행 - 모든 active venue/instrument 에 대해 Properties 기본값 으로 실행 (Scheduler용).
     */
    @Transactional
    public AnomalyScoreDetectResult detectAllActive() {
        List<VenueInstrument> assets = coreMarketDataDao.listActiveVenueInstruments();

        int success = 0;
        int skipped = 0;
        int failed = 0;
        int partial = 0;

        List<AnomalyScoreDetectResult.PerAsset> details = new ArrayList<>(Math.min(assets.size(), 500));

        for (VenueInstrument a : assets) {
            DetectOutcome o = detectOneInternal(
                    a.venueId(),
                    a.instrumentId(),
                    props.getTimeframe(),
                    props.getScoreVersion(),
                    props.getWindowDays()
            );
            switch (o.status) {
                case "success" -> success++;
                case "skipped" -> skipped++;
                case "failed" -> failed++;
                case "partial" -> partial++;
                default -> failed++;
            }
            if (details.size() < 500) {
                details.add(new AnomalyScoreDetectResult.PerAsset(
                        a.venueId(), a.instrumentId(), o.status, o.inputCount, o.outputCount, o.message
                ));
            }
        }

        return new AnomalyScoreDetectResult(assets.size(), success, skipped, failed, partial, details);
    }

    /**
     * Detect 잡(배치) 실행 (RequestBody로 venue/instrument 및 설정값 전달).
     */
    @Transactional
    public AnomalyScoreDetectResult detectAllActive(AnomalyScoreDetectRequest request) {
        EffectiveDetectRequest eff = EffectiveDetectRequest.from(request, props);

        List<VenueInstrument> assets = resolveAssets(eff);
        List<String> timeframes = eff.timeframes;
        List<Integer> windowDaysList = eff.windowDaysList;
        String scoreVersion = eff.scoreVersion;

        long taskCountLong = (long) assets.size() * (long) timeframes.size() * (long) windowDaysList.size();
        int taskCount = (taskCountLong > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) taskCountLong;

        int success = 0;
        int skipped = 0;
        int failed = 0;
        int partial = 0;

        List<AnomalyScoreDetectResult.PerAsset> details = new ArrayList<>(Math.min(taskCount, 500));

        for (VenueInstrument a : assets) {
            for (String timeframe : timeframes) {
                for (int windowDays : windowDaysList) {
                    DetectOutcome o = detectOneInternal(
                            a.venueId(),
                            a.instrumentId(),
                            timeframe,
                            scoreVersion,
                            windowDays
                    );

                    switch (o.status) {
                        case "success" -> success++;
                        case "skipped" -> skipped++;
                        case "failed" -> failed++;
                        case "partial" -> partial++;
                        default -> failed++;
                    }

                    if (details.size() < 500) {
                        String msg = "timeframe=" + timeframe + ", windowDays=" + windowDays + ", message=" + o.message;
                        details.add(new AnomalyScoreDetectResult.PerAsset(
                                a.venueId(), a.instrumentId(), o.status, o.inputCount, o.outputCount, msg
                        ));
                    }
                }
            }
        }

        return new AnomalyScoreDetectResult(taskCount, success, skipped, failed, partial, details);
    }

    private static final class DetectOutcome {

        private final String status;
        private final int inputCount;
        private final int outputCount;
        private final String message;

        private DetectOutcome(String status, int inputCount, int outputCount, String message) {
            this.status = status;
            this.inputCount = inputCount;
            this.outputCount = outputCount;
            this.message = message;
        }
    }

    /**
     * 단일 자산(venue_id, instrument_id)에 대한 이상 점수 계산 (설정값 파라미터화)
     * 증분 기준:
     * - 현재 설정값(timeframe, score_version, window_days) 조합의 마지막 점수 시각을 조회
     * - window_days 도 점수 정의의 일부 이므로, window_days 가 다르면 별도의 증분 기준을 가짐
     * - 예: window_days=90으로 계산된 데이터 가 있어도, window_days=30으로 변경 시 처음 부터 계산
     */
    private DetectOutcome detectOneInternal(
            long venueId,
            long instrumentId,
            String timeframe,
            String scoreVersion,
            int windowDays
    ) {
        if (timeframe == null || timeframe.isBlank()) timeframe = props.getTimeframe();
        if (scoreVersion == null || scoreVersion.isBlank()) scoreVersion = props.getScoreVersion();
        if (windowDays <= 0) windowDays = props.getWindowDays();
        
        int warmupMinDays = props.getWarmupMinDays();
        Integer warmupMinBars = props.getWarmupMinBars();
        int backfillBars = props.getBackfillBars();
        Duration finalCandleSafetyLag = props.getFinalCandleSafetyLag();
        double stdEps = props.getStdEps();
        
        Duration barDuration = parseTimeframeToDuration(timeframe);

        // 현재 설정값(timeframe, score_version, window_days) 조합의 마지막 점수 시각
        OffsetDateTime lastScoreTs = coreMarketDataDao.findLastScoreTs(
                venueId, instrumentId, 
                timeframe, scoreVersion, windowDays
        );
        OffsetDateTime maxCandleTs = coreMarketDataDao.findMaxFinalCandleTs(
                venueId, instrumentId, 
                timeframe, finalCandleSafetyLag
        );

        if (maxCandleTs == null) {
            UUID runId = pipelineRunDao.createDetectRun(
                    venueId, instrumentId, null, null,
                    "{\"reason\":\"no_final_candles\"}"
            );
            pipelineRunDao.finishRun(runId, "skipped", 0, 0, null, null, null, "{\"reason\":\"no_final_candles\"}");
            return new DetectOutcome("skipped", 0, 0, "no_final_candles");
        }

        if (lastScoreTs != null && !lastScoreTs.isBefore(maxCandleTs)) {
            UUID runId = pipelineRunDao.createDetectRun(
                    venueId, instrumentId, lastScoreTs, maxCandleTs,
                    "{\"reason\":\"up_to_date\"}"
            );
            pipelineRunDao.finishRun(runId, "skipped", 0, 0, lastScoreTs, maxCandleTs, null, "{\"reason\":\"up_to_date\"}");
            return new DetectOutcome("skipped", 0, 0, "up_to_date");
        }

        OffsetDateTime initialWriteThreshold = null;
        OffsetDateTime writeStart;
        
        if (lastScoreTs == null) {
            // 초기 실행: 저장 시작을 warmupMinDays만큼 늦춰서 warmup 확보
            // writeStart = max - windowDays + warmupMinDays
            initialWriteThreshold = maxCandleTs
                    .minusDays(windowDays)
                    .plusDays(warmupMinDays);
            writeStart = initialWriteThreshold;
        } else {
            if (backfillBars > 0) {
                writeStart = lastScoreTs.minus(
                    barDuration.multipliedBy(backfillBars)
                );
            } else {
                writeStart = lastScoreTs;
            }
        }
        
        OffsetDateTime readFrom = writeStart
                .minusDays(windowDays)
                .minus(barDuration); // ret 계산용 직전 1봉

        UUID runId = pipelineRunDao.createDetectRun(
                venueId,
                instrumentId,
                writeStart,
                maxCandleTs,
                "{\"score_version\":\"" + scoreVersion + "\",\"window_days\":" + windowDays + "}"
        );

        int inputCount = 0;
        int outputCount = 0;
        String finalStatus = "success";
        String finalExtra = "{}";
        String errorMessage = null;

        try {
            List<CandleRow> candles = coreMarketDataDao.loadFinalCandles(
                    venueId, instrumentId, 
                    timeframe, 
                    readFrom, maxCandleTs
            );
            inputCount = candles.size();

            if (candles.size() < 2) {
                finalStatus = "skipped";
                finalExtra = "{\"reason\":\"insufficient_candles\"}";
                return new DetectOutcome(finalStatus, inputCount, 0, "insufficient_candles");
            }

            RollingWindowStats retStats = new RollingWindowStats(Duration.ofDays(windowDays));
            RollingWindowStats volStats = new RollingWindowStats(Duration.ofDays(windowDays));
            RollingWindowStats rngStats = new RollingWindowStats(Duration.ofDays(windowDays));

            int newCandleCount = 0;
            int skippedByWarmup = 0;

            Double prevClose = null;

            for (int i = 0; i < candles.size(); i++) {
                CandleRow c = candles.get(i);
                OffsetDateTime ts = c.ts();

                // 증분 "저장 대상" 정의
                boolean isNewerThanLastScore;
                if (lastScoreTs == null) {
                    isNewerThanLastScore = true;
                } else if (backfillBars > 0) {
                    OffsetDateTime backfillStart = lastScoreTs.minus(
                        barDuration.multipliedBy(backfillBars)
                    );
                    isNewerThanLastScore = !ts.isBefore(backfillStart);
                } else {
                    isNewerThanLastScore = ts.isAfter(lastScoreTs);
                }
                
                // 초기 실행 시 저장 시작 시점 체크 (warmup 확보를 위해 늦춘 시점)
                boolean isAfterInitialThreshold = (initialWriteThreshold == null) ? true : !ts.isBefore(initialWriteThreshold);
                boolean isWriteTarget = isNewerThanLastScore && isAfterInitialThreshold;

                if (isWriteTarget) newCandleCount++;

                // window eviction
                retStats.evictOlderThan(ts);
                volStats.evictOlderThan(ts);
                rngStats.evictOlderThan(ts);

                // 파생값 계산
                Double ret = calcLogReturn(prevClose, c.close());
                Double logVol = calcLogVol(c.volume());
                Double rng = calcRange(c.high(), c.low(), c.close());

                // z-score는 baseline(이전값들)로 계산 => "현재값"은 아래에서 add
                boolean warmupOk = hasWarmup(retStats, ts, warmupMinDays, warmupMinBars) 
                        && hasWarmup(volStats, ts, warmupMinDays, warmupMinBars) 
                        && hasWarmup(rngStats, ts, warmupMinDays, warmupMinBars);

                Double zRet = zscore(retStats, ret, stdEps);
                Double zVol = zscore(volStats, logVol, stdEps);
                Double zRng = zscore(rngStats, rng, stdEps);
                double score = maxAbs(zRet, zVol, zRng);

                if (isWriteTarget) {
                    if (!warmupOk) {
                        skippedByWarmup++;
                    } else {
                        anomalyScoreUpsertDao.upsert(
                                venueId, instrumentId,
                                timeframe, ts,
                                scoreVersion, windowDays,
                                zRet, zVol, zRng,
                                score,
                                runId
                        );
                        outputCount++;
                    }
                }
                // baseline 업데이트(현재값을 window에 반영)
                retStats.add(ts, ret);
                volStats.add(ts, logVol);
                rngStats.add(ts, rng);

                prevClose = c.close();
            }

            if (outputCount == 0) {
                finalStatus = "skipped";
                finalExtra = "{\"warmup\":true,\"reason\":\"insufficient_baseline\",\"new_candles\":" + newCandleCount + ",\"skipped\":" + skippedByWarmup + "}";
            } else if (outputCount < newCandleCount) {
                finalStatus = "partial";
                finalExtra = "{\"warmup\":true,\"reason\":\"partial_warmup\",\"new_candles\":" + newCandleCount + ",\"skipped\":" + (newCandleCount - outputCount) + "}";
            } else {
                finalStatus = "success";
                finalExtra = "{\"new_candles\":" + newCandleCount + "}";
            }

            return new DetectOutcome(finalStatus, inputCount, outputCount, finalExtra);
        } catch (Exception e) {
            finalStatus = "failed";
            errorMessage = safeMessage(e);
            finalExtra = "{\"reason\":\"exception\"}";
            return new DetectOutcome(finalStatus, inputCount, outputCount, errorMessage);
        } finally {
            pipelineRunDao.finishRun(runId, finalStatus, inputCount, outputCount, writeStart, maxCandleTs, errorMessage, finalExtra);
        }
    }

    private static String safeMessage(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        // 너무 긴 메시지는 pipeline_runs.error_message 저장/로깅에 부담 -> 상한
        if (m.length() > 1000) return m.substring(0, 1000);
        return m;
    }

    /**
     * 요청(RequestBody)을 "단일(legacy) / 배치(list)" 둘 다 수용하기 위해,
     * 내부에서 사용할 유효 값으로 정규화합니다.
     */
    private static final class EffectiveDetectRequest {

        private final boolean batchMode;
        private final Long legacyVenueId;
        private final Long legacyInstrumentId;
        private final List<Long> venueIds;
        private final List<Long> instrumentIds;
        private final List<String> timeframes;
        private final String scoreVersion;
        private final List<Integer> windowDaysList;

        private EffectiveDetectRequest(
                boolean batchMode,
                Long legacyVenueId,
                Long legacyInstrumentId,
                List<Long> venueIds,
                List<Long> instrumentIds,
                List<String> timeframes,
                String scoreVersion,
                List<Integer> windowDaysList
        ) {
            this.batchMode = batchMode;
            this.legacyVenueId = legacyVenueId;
            this.legacyInstrumentId = legacyInstrumentId;
            this.venueIds = venueIds;
            this.instrumentIds = instrumentIds;
            this.timeframes = timeframes;
            this.scoreVersion = scoreVersion;
            this.windowDaysList = windowDaysList;
        }

        private static EffectiveDetectRequest from(AnomalyScoreDetectRequest request, AnomalyScoreProperties props) {
            if (request == null) {
                return new EffectiveDetectRequest(
                        true,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(props.getTimeframe()),
                        props.getScoreVersion(),
                        List.of(30, 60, 90)
                );
            }

            boolean hasAnyList =
                    hasAny(request.getVenueIds())
                            || hasAny(request.getInstrumentIds())
                            || hasAny(request.getTimeframes())
                            || hasAny(request.getWindowDaysList());

            boolean legacySingle = !hasAnyList
                    && request.getVenueId() != null
                    && request.getInstrumentId() != null;

            boolean batchMode = !legacySingle;

            List<Long> venueIds = firstNonEmptyLongs(request.getVenueIds(),
                    (request.getVenueId() != null) ? List.of(request.getVenueId()) : List.of());
            List<Long> instrumentIds = firstNonEmptyLongs(request.getInstrumentIds(),
                    (request.getInstrumentId() != null) ? List.of(request.getInstrumentId()) : List.of());

            List<String> timeframes = firstNonEmptyStrings(request.getTimeframes(),
                    (request.getTimeframe() != null && !request.getTimeframe().isBlank())
                            ? List.of(request.getTimeframe())
                            : List.of(props.getTimeframe()));

            String scoreVersion = (request.getScoreVersion() != null && !request.getScoreVersion().isBlank())
                    ? request.getScoreVersion()
                    : props.getScoreVersion();

            List<Integer> windowDaysList;
            if (hasAny(request.getWindowDaysList())) {
                windowDaysList = normalizePositiveInts(request.getWindowDaysList());
            } else if (request.getWindowDays() != null && request.getWindowDays() > 0) {
                windowDaysList = List.of(request.getWindowDays());
            } else if (batchMode) {
                windowDaysList = List.of(30, 60, 90);
            } else {
                windowDaysList = List.of(props.getWindowDays());
            }

            return new EffectiveDetectRequest(
                    batchMode,
                    request.getVenueId(),
                    request.getInstrumentId(),
                    venueIds,
                    instrumentIds,
                    timeframes,
                    scoreVersion,
                    windowDaysList
            );
        }

        private static boolean hasAny(List<?> xs) {
            return xs != null && !xs.isEmpty();
        }

        private static List<Long> firstNonEmptyLongs(List<Long> primary, List<Long> fallback) {
            if (primary != null && !primary.isEmpty()) return primary;
            return fallback;
        }

        private static List<String> firstNonEmptyStrings(List<String> primary, List<String> fallback) {
            if (primary != null && !primary.isEmpty()) {
                List<String> normalized = new ArrayList<>(primary.size());
                for (String s : primary) {
                    if (s == null) continue;
                    String t = s.trim();
                    if (!t.isEmpty()) normalized.add(t);
                }
                if (!normalized.isEmpty()) return normalized;
            }
            return fallback;
        }

        private static List<Integer> normalizePositiveInts(List<Integer> xs) {
            if (xs == null || xs.isEmpty()) return List.of();
            Set<Integer> set = new LinkedHashSet<>();
            for (Integer x : xs) {
                if (x == null) continue;
                if (x > 0) set.add(x);
            }
            return set.isEmpty() ? List.of() : List.copyOf(set);
        }
    }

    private List<VenueInstrument> resolveAssets(EffectiveDetectRequest eff) {
        // legacy 단일 요청: 지정된 (venueId, instrumentId) 한 건만 처리
        if (!eff.batchMode && eff.legacyVenueId != null && eff.legacyInstrumentId != null) {
            return List.of(new VenueInstrument(eff.legacyVenueId, eff.legacyInstrumentId));
        }

        // 배치 요청: 기본은 active 전체, 필요하면 venue/instrument 필터 적용
        List<VenueInstrument> assets = coreMarketDataDao.listActiveVenueInstruments();

        Set<Long> venueFilter = toFilterSet(eff.venueIds);
        Set<Long> instrumentFilter = toFilterSet(eff.instrumentIds);

        if (venueFilter.isEmpty() && instrumentFilter.isEmpty()) {
            return assets;
        }

        List<VenueInstrument> filtered = new ArrayList<>(assets.size());
        for (VenueInstrument a : assets) {
            boolean ok = true;
            if (!venueFilter.isEmpty() && !venueFilter.contains(a.venueId())) ok = false;
            if (!instrumentFilter.isEmpty() && !instrumentFilter.contains(a.instrumentId())) ok = false;
            if (ok) filtered.add(a);
        }
        return filtered;
    }

    private static Set<Long> toFilterSet(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        Set<Long> set = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) set.add(id);
        }
        return set.isEmpty() ? Set.of() : Set.copyOf(set);
    }

    /**
     * warmup 조건 체크: 시간 기반 + 표본 수 기반 모두 만족해야 함.
     * 시간 기반: earliest 부터 nowTs까지 warmupMinDays 이상
     * 표본 수 기반: warmupMinBars가 설정 되어 있으면 stats.count() >= warmupMinBars
     */
    private boolean hasWarmup(RollingWindowStats stats, OffsetDateTime nowTs, int warmupMinDays, Integer warmupMinBars) {
        OffsetDateTime earliest = stats.earliestTs();
        if (earliest == null) return false;
        
        // 시간 기반 체크
        long days = Duration.between(earliest, nowTs).toDays();
        if (days < warmupMinDays) return false;
        
        // 표본 수 기반 체크 (설정되어 있는 경우)
        if (warmupMinBars != null) {
            if (stats.count() < warmupMinBars) return false;
        }
        
        return true;
    }

    /**
     * z-score 계산.
     * value 가 null 이면 null 을 반환 (0.0이 아님).
     */
    private Double zscore(RollingWindowStats stats, Double value, double stdEps) {
        if (value == null) return null;  // 0.0 대신 null 반환
        if (stats.count() < 2) return null;  // 표본 수 부족도 null
        double std = stats.std();
        if (std < stdEps) return null;  // std가 너무 작으면 null
        double mean = stats.mean();
        return (value - mean) / std;
    }

    /**
     * timeframe 문자열을 Duration으로 파싱
     * 예: "5m" -> Duration.ofMinutes(5), "1h" -> Duration.ofHours(1), "1d" -> Duration.ofDays(1)
     */
    private Duration parseTimeframeToDuration(String timeframe) {
        if (timeframe == null || timeframe.isBlank()) {
            return props.getBarDuration(); // 기본값 사용
        }
        
        timeframe = timeframe.trim().toLowerCase();
        
        if (timeframe.endsWith("m")) {
            try {
                int minutes = Integer.parseInt(timeframe.substring(0, timeframe.length() - 1));
                return Duration.ofMinutes(minutes);
            } catch (NumberFormatException e) {
                return props.getBarDuration();
            }
        } else if (timeframe.endsWith("h")) {
            try {
                int hours = Integer.parseInt(timeframe.substring(0, timeframe.length() - 1));
                return Duration.ofHours(hours);
            } catch (NumberFormatException e) {
                return props.getBarDuration();
            }
        } else if (timeframe.endsWith("d")) {
            try {
                int days = Integer.parseInt(timeframe.substring(0, timeframe.length() - 1));
                return Duration.ofDays(days);
            } catch (NumberFormatException e) {
                return props.getBarDuration();
            }
        } else if (timeframe.endsWith("s")) {
            try {
                int seconds = Integer.parseInt(timeframe.substring(0, timeframe.length() - 1));
                return Duration.ofSeconds(seconds);
            } catch (NumberFormatException e) {
                return props.getBarDuration();
            }
        }
        
        // 파싱 실패 시 기본값 사용
        return props.getBarDuration();
    }

    /**
     * null 을 제외한 z-score 들의 절댓값 중 최대값 반환.
     * null 은 "계산 불가"를 의미 하므로 score 계산 에서 제외됨.
     * 모든 값이 null 이면 0.0 반환.
     */
    private static double maxAbs(Double a, Double b, Double c) {
        double max = 0.0;
        if (a != null) {
            double absA = Math.abs(a);
            if (absA > max) max = absA;
        }
        if (b != null) {
            double absB = Math.abs(b);
            if (absB > max) max = absB;
        }
        if (c != null) {
            double absC = Math.abs(c);
            if (absC > max) max = absC;
        }
        return max;
    }

    private static Double calcLogReturn(Double prevClose, double close) {
        if (prevClose == null) return null;
        if (prevClose <= 0.0 || close <= 0.0) return null;
        return Math.log(close / prevClose);
    }

    private static Double calcLogVol(double volume) {
        if (volume < 0.0) return null;
        return Math.log1p(volume);
    }

    private static Double calcRange(double high, double low, double close) {
        if (close <= 0.0) return null;
        return (high - low) / close;
    }
}

