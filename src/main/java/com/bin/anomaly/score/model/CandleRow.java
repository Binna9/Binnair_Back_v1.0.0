package com.bin.anomaly.score.model;

import java.time.OffsetDateTime;

/**
 * core.candles 조회 결과
 */
public record CandleRow(
        OffsetDateTime ts,
        double high,
        double low,
        double close,
        double volume
) {}

