package com.bin.anomaly.score.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * anomaly score 계산 "정책" 설정.
 */
@Data
@ConfigurationProperties(prefix = "anomaly.score")
public class AnomalyScoreProperties {

    /**
     * Detect 대상 timeframe (기본: 5m)
     */
    private String timeframe = "5m";
    /**
     * timeframe bar duration (ret 계산용 직전 1봉 포함 조회 등에 사용)
     */
    private Duration barDuration = Duration.ofMinutes(5);
    /**
     * anomaly_scores 멱등 키의 일부(UNIQUE에 포함) - 기본: z_v1
     */
    private String scoreVersion = "z_v1";
    /**
     * baseline window (days) - 기본: 90
     */
    private int windowDays = 90;
    /**
     * warm-up 최소 기준(데이터 부족이면 스킵) - 기본: 7일
     */
    private int warmupMinDays = 7;
    /**
     * warm-up 최소 표본 수 (시간 기반 warmup과 함께 사용)
     * 5m 데이터 기준: 7일 * 288봉/일 * 0.7(누락 허용) ≈ 1400
     * 설정하지 않으면 warmupMinDays만 사용
     */
    private Integer warmupMinBars = null;
    /**
     * late-arrival 재계산을 위한 backfill 구간 (봉 개수).
     * 
     * candles가 late-arrival로 수정/재확정될 때(특히 is_final이 바뀔 때),
     * 동일 ts 재계산을 위해 lastScoreTs 이전 backfillBars만큼 재계산.
     * 
     * 기본값: 0 (late-arrival 재계산 안 함, "final candle은 수정 없다" 정책)
     * 권장값: 10~50 (5m 기준 50분~4시간 정도)
     */
    private int backfillBars = 0;
    /**
     * "최신 봉 흔들림" 방지 안전장치: ts <= now() - lag
     */
    private Duration finalCandleSafetyLag = Duration.ofSeconds(300);
    /**
     * std 폭발 방지용 eps
     */
    private double stdEps = 1e-12;
}

