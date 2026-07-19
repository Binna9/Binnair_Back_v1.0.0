package com.bin.anomaly.score.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 고정 기간(time-based) sliding window에 대한 sum/sumsq 기반 평균/표준편차 계산기.
 */
public final class RollingWindowStats {

    private record Sample(long tsEpochMillis, double value) {}

    private final long windowMillis;
    private final Deque<Sample> samples = new ArrayDeque<>();
    private double sum = 0.0;
    private double sumsq = 0.0;

    public RollingWindowStats(Duration window) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.windowMillis = window.toMillis();
    }

    public void evictOlderThan(OffsetDateTime nowTs) {
        if (nowTs == null) return;

        long nowMillis = nowTs.toInstant().toEpochMilli();
        long cutoffMillis = nowMillis - windowMillis;

        while (!samples.isEmpty() && samples.peekFirst().tsEpochMillis() < cutoffMillis) {
            Sample s = samples.removeFirst();
            sum -= s.value();
            sumsq -= s.value() * s.value();
        }
    }

    public void add(OffsetDateTime ts, Double value) {
        if (ts == null || value == null) return;

        long tsMillis = ts.toInstant().toEpochMilli();
        double v = value;
        samples.addLast(new Sample(tsMillis, v));
        sum += v;
        sumsq += v * v;
    }

    public int count() {
        return samples.size();
    }

    public OffsetDateTime earliestTs() {
        if (samples.isEmpty()) return null;
        long earliestMillis = samples.peekFirst().tsEpochMillis();
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(earliestMillis), ZoneOffset.UTC);
    }

    public double mean() {
        int n = samples.size();
        if (n == 0) return 0.0;
        return sum / n;
    }

    public double std() {
        int n = samples.size();
        if (n < 2) return 0.0;
        double mean = sum / n;
        double var = (sumsq - n * mean * mean) / (n - 1);
        if (var < 0.0) var = 0.0;
        return Math.sqrt(var);
    }
}
