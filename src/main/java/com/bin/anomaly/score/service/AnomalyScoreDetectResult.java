package com.bin.anomaly.score.service;

import java.util.List;

public record AnomalyScoreDetectResult(
        int assetCount,
        int successCount,
        int skippedCount,
        int failedCount,
        int partialCount,
        List<PerAsset> details
) {
    public record PerAsset(
            long venueId,
            long instrumentId,
            String status,
            int inputCount,
            int outputCount,
            String message
    ) {}
}

