package com.bin.anomaly.score.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 고정 기간(time-based) sliding window에 대한 sum/sumsq 기반 평균/표준편차 계산기.
 * - 제거(evict)가 가능해야 하므로, 온라인 Welford 대신 sum/sumsq를 사용한다.
 * - 수치 오차로 분산이 음수가 될 수 있어 clamp 처리한다.
 */
final class RollingWindowStats {

    private record Sample(OffsetDateTime ts, double value) {}
    private final Duration window;
    private final Deque<Sample> samples = new ArrayDeque<>();
    private double sum = 0.0;
    private double sumsq = 0.0;

    RollingWindowStats(Duration window) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.window = window;
    }

    void evictOlderThan(OffsetDateTime nowTs) {
        if (nowTs == null) return;
        OffsetDateTime cutoff = nowTs.minus(window);
        while (!samples.isEmpty() && samples.peekFirst().ts().isBefore(cutoff)) {
            Sample s = samples.removeFirst();
            sum -= s.value();
            sumsq -= s.value() * s.value();
        }
    }

    void add(OffsetDateTime ts, Double value) {
        if (ts == null || value == null) return;
        double v = value;
        samples.addLast(new Sample(ts, v));
        sum += v;
        sumsq += v * v;
    }

    int count() {
        return samples.size();
    }

    OffsetDateTime earliestTs() {
        return samples.isEmpty() ? null : samples.peekFirst().ts();
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
        double var = (sumsq / n) - (mean * mean);
        if (var < 0.0) var = 0.0;
        return Math.sqrt(var);
    }
}

