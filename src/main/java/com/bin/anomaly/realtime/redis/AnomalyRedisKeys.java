package com.bin.anomaly.realtime.redis;

public final class AnomalyRedisKeys {

    public static final String READY = "anomaly:meta:ready";
    public static final String UPDATED_AT = "anomaly:meta:updatedAt";
    public static final String WARMING_UP = "anomaly:meta:warmingUp";

    private AnomalyRedisKeys() {}

    public static String series(long venueId, long instrumentId, String timeframe) {
        return "anomaly:series:" + venueId + ":" + instrumentId + ":" + timeframe;
    }

    public static String finals(long venueId, long instrumentId, String timeframe, String mode) {
        return "anomaly:final:" + venueId + ":" + instrumentId + ":" + timeframe + ":" + mode;
    }

    public static String top(String tab, String timeframe, String mode) {
        return "anomaly:top:" + tab.toLowerCase() + ":" + timeframe + ":" + mode;
    }
}
