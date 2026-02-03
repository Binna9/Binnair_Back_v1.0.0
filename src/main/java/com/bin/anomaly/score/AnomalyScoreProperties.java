package com.bin.anomaly.score;

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
     * "최신 봉 흔들림" 방지 안전장치: ts <= now() - lag
     */
    private Duration finalCandleSafetyLag = Duration.ofSeconds(300);
    /**
     * std 폭발 방지용 eps
     */
    private double stdEps = 1e-12;
}

