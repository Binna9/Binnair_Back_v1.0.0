package com.bin.anomaly.score.service;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreFinalResponse;
import com.bin.anomaly.score.repository.CoreMarketDataDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anomaly Score 최종 평가 서비스
 */
@Service
@RequiredArgsConstructor
public class AnomalyScoreFinalService {

    private final CoreMarketDataDao coreMarketDataDao;
    private final AnomalyScoreProperties props;

    // 임계값 상수
    private static final double WATCH_THRESHOLD = 2.0;
    private static final double ANOMALY_THRESHOLD = 3.0;
    private static final double SEVERE_THRESHOLD = 5.0;

    /**
     * 최종 평가 수행
     * 
     * @param venueId 거래소 ID
     * @param instrumentId 종목 ID
     * @param timeframe 캔들 주기 (기본: 5m)
     * @param scoreVersion 점수 버전 (기본: z_v1)
     * @param mode 평가 모드 (max=OR, consensus=AND, 기본: consensus)
     * @param ts 시각 (없으면 최신 공통 ts 사용)
     * @return 최종 평가 결과
     */
    public AnomalyScoreFinalResponse evaluateFinal(
            long venueId,
            long instrumentId,
            String timeframe,
            String scoreVersion,
            String mode,
            OffsetDateTime ts
    ) {
        // 기본값 설정
        String tf = (timeframe == null || timeframe.isBlank()) ? props.getTimeframe() : timeframe;
        String sv = (scoreVersion == null || scoreVersion.isBlank()) ? props.getScoreVersion() : scoreVersion;
        String evalMode = (mode == null || mode.isBlank()) ? "consensus" : mode.toLowerCase();

        // ts가 없으면 최신 공통 ts 조회
        OffsetDateTime targetTs = ts;
        if (targetTs == null) {
            targetTs = coreMarketDataDao.findLatestCommonTs(venueId, instrumentId, tf, sv);
            if (targetTs == null) {
                throw new IllegalArgumentException("No common timestamp found for windows 30, 60, 90");
            }
        }

        // 해당 ts의 3개 window 로우 조회
        List<CoreMarketDataDao.WindowScoreRow> rows = coreMarketDataDao.loadWindowScores(
                venueId, instrumentId, tf, sv, targetTs
        );

        if (rows.size() != 3) {
            throw new IllegalArgumentException(
                    String.format("Expected 3 window rows (30, 60, 90), but found %d", rows.size())
            );
        }

        Map<Integer, CoreMarketDataDao.WindowScoreRow> windowMap = new HashMap<>();
        for (CoreMarketDataDao.WindowScoreRow row : rows) {
            windowMap.put(row.windowDays(), row);
        }

        Double s30 = getScore(windowMap.get(30));
        Double s60 = getScore(windowMap.get(60));
        Double s90 = getScore(windowMap.get(90));

        // 모드별 최종 점수 계산
        Double finalScore;
        String basis;

        switch (evalMode) {
            case "max" -> {
                finalScore = maxNonNull(s30, s60, s90);
                basis = "MAX_30_60_90";
            }
            case "consensus" -> {
                Double c1 = minNonNull(s30, s60);
                Double c2 = minNonNull(s60, s90);
                finalScore = maxNonNull(c1, c2);

                if (c1 != null && c2 != null) {
                    basis = (c1 >= c2) ? "CONSENSUS_30_60" : "CONSENSUS_60_90";
                } else if (c1 != null) {
                    basis = "CONSENSUS_30_60";
                } else if (c2 != null) {
                    basis = "CONSENSUS_60_90";
                } else {
                    basis = "INSUFFICIENT_DATA";
                }
            }
            default -> throw new IllegalStateException("Unreachable: " + evalMode);
        }

        // finalLevel 계산
        String finalLevel = calculateFinalLevel(finalScore);

        // components 생성
        List<AnomalyScoreFinalResponse.Component> components = new ArrayList<>();
        for (int windowDays : new int[]{30, 60, 90}) {
            CoreMarketDataDao.WindowScoreRow row = windowMap.get(windowDays);
            if (row != null) {
                String driver = calculateDriver(row.zRet(), row.zVol(), row.zRng());
                components.add(new AnomalyScoreFinalResponse.Component(
                        windowDays,
                        row.score(),
                        driver,
                        row.zRet(),
                        row.zVol(),
                        row.zRng()
                ));
            }
        }

        OffsetDateTime responseTs = targetTs
                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime();

        return new AnomalyScoreFinalResponse(
                responseTs,
                evalMode,
                finalScore,
                finalLevel,
                basis,
                components
        );
    }

    /**
     * WindowScoreRow에서 score 추출 (null 처리)
     */
    private Double getScore(CoreMarketDataDao.WindowScoreRow row) {
        return row != null ? row.score() : null;
    }

    /**
     * null을 제외한 최대값
     */
    private Double maxNonNull(Double... values) {
        Double max = null;
        for (Double v : values) {
            if (v != null) {
                if (max == null || v > max) {
                    max = v;
                }
            }
        }
        return max;
    }

    /**
     * null을 제외한 최소값
     */
    private Double minNonNull(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    /**
     * Driver 계산 (z_ret, z_vol, z_rng 중 절댓값 이 가장 큰 것)
     */
    private String calculateDriver(Double zRet, Double zVol, Double zRng) {
        double maxAbs = 0.0;
        String driver = "NONE";

        if (zRet != null) {
            double absRet = Math.abs(zRet);
            if (absRet > maxAbs) {
                maxAbs = absRet;
                driver = "RET";
            }
        }
        if (zVol != null) {
            double absVol = Math.abs(zVol);
            if (absVol > maxAbs) {
                maxAbs = absVol;
                driver = "VOL";
            }
        }
        if (zRng != null) {
            double absRng = Math.abs(zRng);
            if (absRng > maxAbs) {
                maxAbs = absRng;
                driver = "RNG";
            }
        }

        return driver;
    }

    /**
     * FinalLevel 계산
     */
    private String calculateFinalLevel(Double finalScore) {
        if (finalScore == null) {
            return "NORMAL";
        }
        if (finalScore >= SEVERE_THRESHOLD) {
            // SEVERE   : 구조적/비정상 상태 가능성 높음
            return "SEVERE";
        } else if (finalScore >= ANOMALY_THRESHOLD) {
            // ANOMALY  : 통계적 이상 확정
            return "ANOMALY";
        } else if (finalScore >= WATCH_THRESHOLD) {
            // WATCH    : 이상 징후 관찰 필요
            return "WATCH";
        } else {
            return "NORMAL";
        }
    }
}
