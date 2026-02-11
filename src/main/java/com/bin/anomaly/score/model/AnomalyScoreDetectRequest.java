package com.bin.anomaly.score.model;

import lombok.Data;

import java.util.List;

/**
 * Anomaly Score Detect API 요청 DTO
 * timeframe, scoreVersion, windowDays는 optional 이며, 값이 없으면 AnomalyScoreProperties의 기본 값이 사용 됩니다.
 * warmupMinDays, warmupMinBars, backfillBars, finalCandleSafetyLag, stdEps는 항상 기본 값을 사용 합니다.
 */
@Data
public class AnomalyScoreDetectRequest {

    /**
     * (LEGACY) venue ID (단일)
     * 기존 단일 실행 요청 바디와의 호환을 위해 유지합니다.
     */
    private Long venueId;

    /**
     * (LEGACY) instrument ID (단일)
     * 기존 단일 실행 요청 바디와의 호환을 위해 유지합니다.
     */
    private Long instrumentId;

    /**
     * (LEGACY) Detect 대상 timeframe (단일, 예: "5m")
     * 기존 단일 실행 요청 바디와의 호환을 위해 유지합니다.
     */
    private String timeframe;

    /**
     * anomaly_scores 멱등 키의 일부(UNIQUE에 포함) (예: "z_v1")
     * 기본값: "z_v1"
     */
    private String scoreVersion;

    /**
     * (LEGACY) baseline window (days) 단일
     * 기존 단일 실행 요청 바디와의 호환을 위해 유지합니다.
     */
    private Integer windowDays;

    /**
     * Detect 대상 venue ID 목록.
     * 비어 있으면 "전체 active venue/instrument"를 대상으로 동작합니다. (API 배치 모드)
     */
    private List<Long> venueIds;

    /**
     * Detect 대상 instrument ID 목록.
     * 비어 있으면 "전체 active venue/instrument"를 대상으로 동작합니다. (API 배치 모드)
     */
    private List<Long> instrumentIds;

    /**
     * Detect 대상 timeframe 목록 (예: ["5m", "1h"]).
     * 비어 있으면 AnomalyScoreProperties 기본값(props.timeframe)만 사용합니다.
     */
    private List<String> timeframes;

    /**
     * baseline window (days) 목록 (예: [30, 60, 90]).
     * 비어 있으면 (배치 모드일 때) 기본값 [30, 60, 90]을 사용합니다.
     * 단일(legacy) 모드일 때는 props.windowDays 기본값을 사용합니다.
     */
    private List<Integer> windowDaysList;
}
