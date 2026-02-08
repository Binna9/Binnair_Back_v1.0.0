package com.bin.anomaly.score.model;

import lombok.Data;

/**
 * Anomaly Score Detect API 요청 DTO
 * timeframe, scoreVersion, windowDays는 optional 이며, 값이 없으면 AnomalyScoreProperties의 기본 값이 사용 됩니다.
 * warmupMinDays, warmupMinBars, backfillBars, finalCandleSafetyLag, stdEps는 항상 기본 값을 사용 합니다.
 */
@Data
public class AnomalyScoreDetectRequest {

    /**
     * venue ID (필수)
     */
    private Long venueId;

    /**
     * instrument ID (필수)
     */
    private Long instrumentId;

    /**
     * Detect 대상 timeframe (예: "5m")
     * 기본값: "5m"
     */
    private String timeframe;

    /**
     * anomaly_scores 멱등 키의 일부(UNIQUE에 포함) (예: "z_v1")
     * 기본값: "z_v1"
     */
    private String scoreVersion;

    /**
     * baseline window (days)
     * 기본값: 90
     */
    private Integer windowDays;
}
