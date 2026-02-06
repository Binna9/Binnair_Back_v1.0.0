package com.bin.anomaly.score.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 단일 자산(venueId/instrumentId)의 캔들(OHLCV) + 이상점수(score/z_*) 시계열 응답.
 * - 차트에서 동일 ts 축으로 가격/거래량/score를 함께 그리기 위한 포맷.
 */
public record AnomalyScoreSeriesResponse(
        Meta meta,
        Summary summary,
        List<Point> points
) {
    public record Meta(
            long venueId,
            String venueCode,
            long instrumentId,
            String instrumentSymbol,
            String venueSymbol,
            String timeframe,
            String scoreVersion,
            Integer windowDays,
            OffsetDateTime from,
            OffsetDateTime to,
            OffsetDateTime serverTime,
            int count
    ) {}

    public record Summary(
            OffsetDateTime latestTs,
            Double latestScore,
            Double maxScore,
            OffsetDateTime maxScoreTs
    ) {}

    public record Point(
            OffsetDateTime ts,
            double o,
            double h,
            double l,
            double c,
            double v,
            Double score,
            Double zRet,
            Double zVol,
            Double zRng,
            String driver
    ) {}
}

