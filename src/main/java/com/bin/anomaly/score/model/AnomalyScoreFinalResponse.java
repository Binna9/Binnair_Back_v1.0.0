package com.bin.anomaly.score.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Anomaly Score 최종 평가 API 응답
 */
public record AnomalyScoreFinalResponse(
        OffsetDateTime ts,
        String mode,
        Double finalScore,
        String finalLevel,
        String basis,
        List<Component> components
) {
    /**
     * 각 window별 점수 컴포넌트
     */
    public record Component(
            int windowDays,
            Double score,
            String driver,
            Double zRet,
            Double zVol,
            Double zRng
    ) {}
}
