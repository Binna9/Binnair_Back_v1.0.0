package com.bin.anomaly.score.service;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * anomaly z-score / final / driver 공통 수식.
 */
public final class AnomalyScoreMath {

    public static final double WATCH_THRESHOLD = 2.0;
    public static final double ANOMALY_THRESHOLD = 3.0;
    public static final double SEVERE_THRESHOLD = 5.0;

    private AnomalyScoreMath() {}

    public static Double calcLogReturn(Double prevClose, double close) {
        if (prevClose == null) return null;
        if (prevClose <= 0.0 || close <= 0.0) return null;
        return Math.log(close / prevClose);
    }

    public static Double calcLogVol(double volume) {
        if (volume < 0.0) return null;
        return Math.log1p(volume);
    }

    public static Double calcRange(double high, double low, double close) {
        if (close <= 0.0) return null;
        return (high - low) / close;
    }

    public static boolean hasWarmup(RollingWindowStats stats, OffsetDateTime nowTs, int warmupMinDays, Integer warmupMinBars) {
        OffsetDateTime earliest = stats.earliestTs();
        if (earliest == null) return false;
        long days = Duration.between(earliest, nowTs).toDays();
        if (days < warmupMinDays) return false;
        if (warmupMinBars != null && stats.count() < warmupMinBars) return false;
        return true;
    }

    public static Double zscore(RollingWindowStats stats, Double value, double stdEps) {
        if (value == null) return null;
        if (stats.count() < 2) return null;
        double std = stats.std();
        if (std < stdEps) return null;
        double mean = stats.mean();
        return (value - mean) / std;
    }

    public static double maxAbs(Double a, Double b, Double c) {
        double max = 0.0;
        if (a != null) max = Math.max(max, Math.abs(a));
        if (b != null) max = Math.max(max, Math.abs(b));
        if (c != null) max = Math.max(max, Math.abs(c));
        return max;
    }

    public static String driverOf(Double zRet, Double zVol, Double zRng) {
        double a = zRet == null ? -1.0 : Math.abs(zRet);
        double b = zVol == null ? -1.0 : Math.abs(zVol);
        double c = zRng == null ? -1.0 : Math.abs(zRng);
        if (a < 0 && b < 0 && c < 0) return null;
        if (a >= b && a >= c) return "RET";
        if (b >= a && b >= c) return "VOL";
        return "RNG";
    }

    public static String driverOfLower(Double zRet, Double zVol, Double zRng) {
        String d = driverOf(zRet, zVol, zRng);
        return d == null ? null : d.toLowerCase();
    }

    public static String finalLevel(Double finalScore) {
        if (finalScore == null) return "NORMAL";
        if (finalScore >= SEVERE_THRESHOLD) return "SEVERE";
        if (finalScore >= ANOMALY_THRESHOLD) return "ANOMALY";
        if (finalScore >= WATCH_THRESHOLD) return "WATCH";
        return "NORMAL";
    }

    public static int severityRank(String level) {
        if (level == null) return 0;
        return switch (level.toUpperCase()) {
            case "SEVERE" -> 3;
            case "ANOMALY" -> 2;
            case "WATCH" -> 1;
            default -> 0;
        };
    }

    public static Double maxNonNull(Double... values) {
        Double max = null;
        for (Double v : values) {
            if (v != null && (max == null || v > max)) max = v;
        }
        return max;
    }

    public static Double minNonNull(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    public static FinalAgg evaluateFinal(Double s30, Double s60, Double s90, String mode) {
        String evalMode = (mode == null || mode.isBlank()) ? "consensus" : mode.toLowerCase();
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
            default -> throw new IllegalArgumentException("unsupported mode: " + evalMode);
        }
        return new FinalAgg(evalMode, finalScore, finalLevel(finalScore), basis);
    }

    public record FinalAgg(String mode, Double finalScore, String finalLevel, String basis) {}

    public static Duration parseTimeframeToDuration(String timeframe, Duration fallback) {
        if (timeframe == null || timeframe.isBlank()) return fallback;
        String tf = timeframe.trim().toLowerCase();
        try {
            if (tf.endsWith("m")) return Duration.ofMinutes(Integer.parseInt(tf.substring(0, tf.length() - 1)));
            if (tf.endsWith("h")) return Duration.ofHours(Integer.parseInt(tf.substring(0, tf.length() - 1)));
            if (tf.endsWith("d")) return Duration.ofDays(Integer.parseInt(tf.substring(0, tf.length() - 1)));
            if (tf.endsWith("s")) return Duration.ofSeconds(Integer.parseInt(tf.substring(0, tf.length() - 1)));
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }
}
