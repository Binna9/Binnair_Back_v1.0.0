package com.bin.anomaly.score.service;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnomalyScoreDetectService {

    private final CoreMarketDataDao coreMarketDataDao;
    private final PipelineRunDao pipelineRunDao;
    private final AnomalyScoreUpsertDao anomalyScoreUpsertDao;
    private final AnomalyScoreProperties props;

    /**
     * Detect 잡(배치) 실행.
     * - 입력 범위 고정: (is_active + timeframe=5m + is_final + safety_lag)
     * - 증분 기준점 고정: last_score_ts
     * - baseline 포함 로딩/신규만 저장
     * - warm-up: baseline 7일 미만은 스킵
     * - 멱등 저장: ON CONFLICT upsert
     * - pipeline_runs 기록
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
            DetectOutcome o = detectOne(a.venueId(), a.instrumentId());
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

    private DetectOutcome detectOne(long venueId, long instrumentId) {

        OffsetDateTime lastScoreTs = coreMarketDataDao.findLastScoreTs(venueId, instrumentId);
        OffsetDateTime maxCandleTs = coreMarketDataDao.findMaxFinalCandleTs(venueId, instrumentId);

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

        // 초기(점수 0건)면: last_score_ts = NULL로 보고 warm-up 정책 적용
        OffsetDateTime initialWriteThreshold = (lastScoreTs == null)
                ? maxCandleTs.minusDays(props.getWindowDays())
                : null;

        OffsetDateTime writeStart = (lastScoreTs == null) ? initialWriteThreshold : lastScoreTs;
        OffsetDateTime readFrom = writeStart
                .minusDays(props.getWindowDays())
                .minus(props.getBarDuration()); // ret 계산용 직전 1봉

        UUID runId = pipelineRunDao.createDetectRun(
                venueId,
                instrumentId,
                writeStart,
                maxCandleTs,
                "{\"score_version\":\"" + props.getScoreVersion() + "\",\"window_days\":" + props.getWindowDays() + "}"
        );

        int inputCount = 0;
        int outputCount = 0;
        String finalStatus = "success";
        String finalExtra = "{}";
        String errorMessage = null;

        try {
            List<CandleRow> candles = coreMarketDataDao.loadFinalCandles(venueId, instrumentId, readFrom, maxCandleTs);
            inputCount = candles.size();

            if (candles.size() < 2) {
                finalStatus = "skipped";
                finalExtra = "{\"reason\":\"insufficient_candles\"}";
                return new DetectOutcome(finalStatus, inputCount, 0, "insufficient_candles");
            }

            RollingWindowStats retStats = new RollingWindowStats(Duration.ofDays(props.getWindowDays()));
            RollingWindowStats volStats = new RollingWindowStats(Duration.ofDays(props.getWindowDays()));
            RollingWindowStats rngStats = new RollingWindowStats(Duration.ofDays(props.getWindowDays()));

            int newCandleCount = 0;
            int skippedByWarmup = 0;

            Double prevClose = null;

            for (int i = 0; i < candles.size(); i++) {
                CandleRow c = candles.get(i);
                OffsetDateTime ts = c.ts();

                // 증분 "저장 대상" 정의
                boolean isNewerThanLastScore = (lastScoreTs == null) ? true : ts.isAfter(lastScoreTs);
                boolean isAfterInitialThreshold = (initialWriteThreshold == null) ? true : ts.isAfter(initialWriteThreshold);
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
                boolean warmupOk = hasWarmup(retStats, ts) && hasWarmup(volStats, ts) && hasWarmup(rngStats, ts);

                Double zRet = zscore(retStats, ret);
                Double zVol = zscore(volStats, logVol);
                Double zRng = zscore(rngStats, rng);
                double score = maxAbs(zRet, zVol, zRng);

                if (isWriteTarget) {
                    if (!warmupOk) {
                        skippedByWarmup++;
                    } else {
                        anomalyScoreUpsertDao.upsert(
                                venueId, instrumentId, ts,
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

    private boolean hasWarmup(RollingWindowStats stats, OffsetDateTime nowTs) {
        OffsetDateTime earliest = stats.earliestTs();
        if (earliest == null) return false;
        long days = Duration.between(earliest, nowTs).toDays();
        return days >= props.getWarmupMinDays();
    }

    private Double zscore(RollingWindowStats stats, Double value) {
        if (value == null) return 0.0;
        if (stats.count() < 2) return 0.0;
        double std = stats.std();
        if (std < props.getStdEps()) return 0.0;
        double mean = stats.mean();
        return (value - mean) / std;
    }

    private static double maxAbs(Double a, Double b, Double c) {
        double x = a == null ? 0.0 : Math.abs(a);
        double y = b == null ? 0.0 : Math.abs(b);
        double z = c == null ? 0.0 : Math.abs(c);
        return Math.max(x, Math.max(y, z));
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

