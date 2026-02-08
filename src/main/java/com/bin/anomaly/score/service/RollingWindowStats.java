package com.bin.anomaly.score.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 고정 기간(time-based) sliding window에 대한 sum/sumsq 기반 평균/표준편차 계산기.
 * 성능 최적화: OffsetDateTime 객체 대신 long epochMillis를 사용하여 GC 부담 감소.
 * 자산 수가 많을 때(수십~수백 개) 메모리 효율적.
 */
final class RollingWindowStats {

    /**
     * 샘플 데이터.
     * ts는 epochMillis (UTC 기준)로 저장 하여 메모리 효율성 향상.
     */
    private record Sample(long tsEpochMillis, double value) {}
    
    private final long windowMillis;
    private final Deque<Sample> samples = new ArrayDeque<>();
    private double sum = 0.0;
    private double sumsq = 0.0;

    RollingWindowStats(Duration window) {

        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }

        this.windowMillis = window.toMillis();
    }

    /**
     * nowTs 이전 window 기간 밖의 샘플 제거.
     * 
     * @param nowTs 현재 시각 (OffsetDateTime)
     */
    void evictOlderThan(OffsetDateTime nowTs) {

        if (nowTs == null) return;

        long nowMillis = nowTs.toInstant().toEpochMilli();
        long cutoffMillis = nowMillis - windowMillis;

        while (!samples.isEmpty() && samples.peekFirst().tsEpochMillis() < cutoffMillis) {
            Sample s = samples.removeFirst();
            sum -= s.value();
            sumsq -= s.value() * s.value();
        }
    }

    /**
     * 샘플 추가.
     * 
     * @param ts 시각 (OffsetDateTime)
     * @param value 값
     */
    void add(OffsetDateTime ts, Double value) {

        if (ts == null || value == null) return;

        long tsMillis = ts.toInstant().toEpochMilli();
        double v = value;
        samples.addLast(new Sample(tsMillis, v));
        sum += v;
        sumsq += v * v;
    }

    int count() {
        return samples.size();
    }

    /**
     * 가장 오래된 샘플의 시각 반환.
     * 
     * @return 가장 오래된 시각 (없으면 null)
     */
    OffsetDateTime earliestTs() {
        if (samples.isEmpty()) return null;
        long earliestMillis = samples.peekFirst().tsEpochMillis();
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(earliestMillis), ZoneOffset.UTC);
    }

    double mean() {

        int n = samples.size();

        if (n == 0) return 0.0;

        return sum / n;
    }

    double std() {

        int n = samples.size();

        if (n < 2) return 0.0;

        double mean = sum / n;

        double var = (sumsq - n * mean * mean) / (n - 1);

        if (var < 0.0) var = 0.0;

        return Math.sqrt(var);
    }
}

