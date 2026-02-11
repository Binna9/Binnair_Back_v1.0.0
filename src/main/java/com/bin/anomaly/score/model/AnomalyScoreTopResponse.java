package com.bin.anomaly.score.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * "지금 가장 이상한 종목 Top N" 응답 DTO.
 */
public record AnomalyScoreTopResponse(
        String timeframe,
        String mode,
        /**
         * 탭 구분.
         * - AGG: 종합 이상(finalScore 기준)
         * - VOL: 거래량 이상(z_vol 기반)
         * - RNG: 변동폭 이상(z_rng 기반)
         * - RET: 급등/급락(z_ret 기반)
         */
        String tab,
        int limit,
        int deltaBars,
        /**
         * 대표 ts (현재 구현에서는 1위 아이템의 ts).
         * 각 종목별 최신 공통 ts는 items[].ts 를 참고하세요.
         */
        OffsetDateTime ts,
        List<Item> items
) {
    public record Item(
            int rank,
            long venueId,
            long instrumentId,
            String symbol,
            OffsetDateTime ts,
            String finalLevel,
            Double finalScore,
            String driver,
            /**
             * 탭별 핵심 지표(정렬 키).
             * - AGG: finalScore
             * - VOL: z_vol 기반 합성값
             * - RNG: z_rng 기반 합성값
             * - RET: |z_ret| 기반 합성값
             */
            Double metricValue,
            /**
             * Δ = metricValue(now) - metricValue(prev), prev는 deltaBars 봉 전 공통 ts.
             */
            Double delta,
            /**
             * RET 탭에서만 사용: UP/DOWN/MIXED/FLAT
             */
            String direction
    ) {}
}

