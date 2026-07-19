package com.bin.anomaly.realtime.model;

import java.time.OffsetDateTime;

/**
 * 거래소에서 받은 OHLCV 봉.
 */
public record MarketCandle(
        OffsetDateTime ts,
        double open,
        double high,
        double low,
        double close,
        double volume,
        boolean isFinal
) {}
