package com.bin.anomaly.score.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * "스캐너(Top N)" 전용 조회 DAO.
 *
 * 핵심:
 * - (venue_id, instrument_id)별로 windowDays(30/60/90)가 모두 존재하는 "공통 ts"를 만들고
 * - 그 중 최신 ts(=rn=1)와 n봉 전 ts(=rn=deltaBars+1)를 뽑아서
 * - mode(consensus/max) 기준 finalScore를 DB에서 계산 후 정렬/limit.
 */
@Repository
@RequiredArgsConstructor
public class AnomalyScoreScannerDao {

    @PersistenceContext
    private final EntityManager em;

    public record TopRow(
            int rank,
            long venueId,
            long instrumentId,
            String symbol,
            OffsetDateTime ts,
            String finalLevel,
            Double finalScore,
            String driver,
            Double metricValue,
            Double delta,
            String direction
    ) {}

    /**
     * 탭별 Top N 조회.
     *
     * @param tab          AGG/VOL/RNG/RET
     * @param timeframe    timeframe
     * @param scoreVersion scoreVersion
     * @param mode         consensus/max
     * @param limit        상위 N
     * @param deltaBars    Δ 기준 이전 봉 수(공통 ts 기준)
     * @param minSeverity  최소 심각도 필터 (null이면 미적용) 0=NORMAL, 1=WATCH, 2=ANOMALY, 3=SEVERE
     * @param driverFilter driver 필터 (RET/VOL/RNG, null이면 미적용)
     * @param minDeltaAbs  |Δ| 최소값 필터 (null이면 미적용)
     */
    public List<TopRow> listTop(
            String tab,
            String timeframe,
            String scoreVersion,
            String mode,
            int limit,
            int deltaBars,
            Integer minSeverity,
            String driverFilter,
            Double minDeltaAbs
    ) {
        int prevRn = deltaBars + 1;

        // mode는 SQL CASE에서 사용되므로 2가지만 허용
        String m = (mode == null) ? "consensus" : mode.trim().toLowerCase();
        if (!m.equals("consensus") && !m.equals("max")) {
            throw new IllegalArgumentException("mode must be consensus or max");
        }

        String t = (tab == null) ? "AGG" : tab.trim().toUpperCase();
        if (!t.equals("AGG") && !t.equals("VOL") && !t.equals("RNG") && !t.equals("RET")) {
            throw new IllegalArgumentException("tab must be AGG/VOL/RNG/RET");
        }

        String sql = """
                WITH common_ts AS (
                    SELECT s.venue_id, s.instrument_id, s.ts
                    FROM core.anomaly_scores s
                    WHERE s.timeframe = :timeframe
                      AND s.score_version = :scoreVersion
                      AND s.window_days IN (30, 60, 90)
                    GROUP BY s.venue_id, s.instrument_id, s.ts
                    HAVING COUNT(*) = 3
                ),
                ranked_ts AS (
                    SELECT
                        venue_id, instrument_id, ts,
                        ROW_NUMBER() OVER (PARTITION BY venue_id, instrument_id ORDER BY ts DESC) AS rn
                    FROM common_ts
                ),
                picked_ts AS (
                    SELECT
                        venue_id,
                        instrument_id,
                        MAX(CASE WHEN rn = 1 THEN ts END) AS ts_now,
                        MAX(CASE WHEN rn = :prevRn THEN ts END) AS ts_prev
                    FROM ranked_ts
                    WHERE rn = 1 OR rn = :prevRn
                    GROUP BY venue_id, instrument_id
                ),
                now_scores AS (
                    SELECT
                        s.venue_id,
                        s.instrument_id,
                        s.ts,
                        MAX(CASE WHEN s.window_days = 30 THEN s.score END) AS s30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.score END) AS s60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.score END) AS s90,
                        MAX(CASE WHEN s.window_days = 30 THEN s.z_ret END) AS zret30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.z_ret END) AS zret60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.z_ret END) AS zret90,
                        MAX(CASE WHEN s.window_days = 30 THEN s.z_vol END) AS zvol30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.z_vol END) AS zvol60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.z_vol END) AS zvol90,
                        MAX(CASE WHEN s.window_days = 30 THEN s.z_rng END) AS zrng30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.z_rng END) AS zrng60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.z_rng END) AS zrng90
                    FROM core.anomaly_scores s
                    JOIN picked_ts p
                      ON p.venue_id = s.venue_id
                     AND p.instrument_id = s.instrument_id
                     AND p.ts_now = s.ts
                    WHERE s.timeframe = :timeframe
                      AND s.score_version = :scoreVersion
                      AND s.window_days IN (30, 60, 90)
                    GROUP BY s.venue_id, s.instrument_id, s.ts
                ),
                prev_scores AS (
                    SELECT
                        s.venue_id,
                        s.instrument_id,
                        s.ts,
                        MAX(CASE WHEN s.window_days = 30 THEN s.score END) AS s30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.score END) AS s60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.score END) AS s90,
                        MAX(CASE WHEN s.window_days = 30 THEN s.z_ret END) AS zret30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.z_ret END) AS zret60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.z_ret END) AS zret90,
                        MAX(CASE WHEN s.window_days = 30 THEN s.z_vol END) AS zvol30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.z_vol END) AS zvol60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.z_vol END) AS zvol90,
                        MAX(CASE WHEN s.window_days = 30 THEN s.z_rng END) AS zrng30,
                        MAX(CASE WHEN s.window_days = 60 THEN s.z_rng END) AS zrng60,
                        MAX(CASE WHEN s.window_days = 90 THEN s.z_rng END) AS zrng90
                    FROM core.anomaly_scores s
                    JOIN picked_ts p
                      ON p.venue_id = s.venue_id
                     AND p.instrument_id = s.instrument_id
                     AND p.ts_prev = s.ts
                    WHERE s.timeframe = :timeframe
                      AND s.score_version = :scoreVersion
                      AND s.window_days IN (30, 60, 90)
                    GROUP BY s.venue_id, s.instrument_id, s.ts
                ),
                scored AS (
                    SELECT
                        n.venue_id,
                        n.instrument_id,
                        n.ts AS ts,
                        i.symbol AS symbol,
                        -- finalScore(mode)
                        CASE
                            WHEN :mode = 'max'
                                THEN GREATEST(n.s30, n.s60, n.s90)
                            ELSE
                                GREATEST(LEAST(n.s30, n.s60), LEAST(n.s60, n.s90))
                        END AS final_score,
                        CASE
                            WHEN :mode = 'max'
                                THEN GREATEST(p.s30, p.s60, p.s90)
                            ELSE
                                GREATEST(LEAST(p.s30, p.s60), LEAST(p.s60, p.s90))
                        END AS prev_final_score,
                        -- 대표 driver: window(30/60/90) 통합해서 RET/VOL/RNG 중 가장 큰 축
                        GREATEST(ABS(COALESCE(n.zret30, 0.0)), ABS(COALESCE(n.zret60, 0.0)), ABS(COALESCE(n.zret90, 0.0))) AS ret_max,
                        GREATEST(ABS(COALESCE(n.zvol30, 0.0)), ABS(COALESCE(n.zvol60, 0.0)), ABS(COALESCE(n.zvol90, 0.0))) AS vol_max,
                        GREATEST(ABS(COALESCE(n.zrng30, 0.0)), ABS(COALESCE(n.zrng60, 0.0)), ABS(COALESCE(n.zrng90, 0.0))) AS rng_max,
                        -- 탭별 metricValue(now/prev) 계산 (z 기반, mode 적용)
                        CASE
                            WHEN :tab = 'AGG' THEN
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(n.s30, n.s60, n.s90)
                                    ELSE GREATEST(LEAST(n.s30, n.s60), LEAST(n.s60, n.s90))
                                END
                            WHEN :tab = 'VOL' THEN
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(ABS(n.zvol30), ABS(n.zvol60), ABS(n.zvol90))
                                    ELSE GREATEST(LEAST(ABS(n.zvol30), ABS(n.zvol60)), LEAST(ABS(n.zvol60), ABS(n.zvol90)))
                                END
                            WHEN :tab = 'RNG' THEN
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(ABS(n.zrng30), ABS(n.zrng60), ABS(n.zrng90))
                                    ELSE GREATEST(LEAST(ABS(n.zrng30), ABS(n.zrng60)), LEAST(ABS(n.zrng60), ABS(n.zrng90)))
                                END
                            ELSE
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(ABS(n.zret30), ABS(n.zret60), ABS(n.zret90))
                                    ELSE GREATEST(LEAST(ABS(n.zret30), ABS(n.zret60)), LEAST(ABS(n.zret60), ABS(n.zret90)))
                                END
                        END AS metric_now,
                        CASE
                            WHEN p.venue_id IS NULL THEN NULL
                            WHEN :tab = 'AGG' THEN
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(p.s30, p.s60, p.s90)
                                    ELSE GREATEST(LEAST(p.s30, p.s60), LEAST(p.s60, p.s90))
                                END
                            WHEN :tab = 'VOL' THEN
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(ABS(p.zvol30), ABS(p.zvol60), ABS(p.zvol90))
                                    ELSE GREATEST(LEAST(ABS(p.zvol30), ABS(p.zvol60)), LEAST(ABS(p.zvol60), ABS(p.zvol90)))
                                END
                            WHEN :tab = 'RNG' THEN
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(ABS(p.zrng30), ABS(p.zrng60), ABS(p.zrng90))
                                    ELSE GREATEST(LEAST(ABS(p.zrng30), ABS(p.zrng60)), LEAST(ABS(p.zrng60), ABS(p.zrng90)))
                                END
                            ELSE
                                CASE
                                    WHEN :mode = 'max' THEN GREATEST(ABS(p.zret30), ABS(p.zret60), ABS(p.zret90))
                                    ELSE GREATEST(LEAST(ABS(p.zret30), ABS(p.zret60)), LEAST(ABS(p.zret60), ABS(p.zret90)))
                                END
                        END AS metric_prev,
                        -- RET 방향 (RET 탭에서만 의미 있음)
                        CASE
                            WHEN :tab <> 'RET' THEN NULL
                            WHEN :mode = 'max' THEN
                                CASE
                                    WHEN ABS(n.zret30) >= ABS(n.zret60) AND ABS(n.zret30) >= ABS(n.zret90) THEN
                                        CASE WHEN n.zret30 > 0 THEN 'UP' WHEN n.zret30 < 0 THEN 'DOWN' ELSE 'FLAT' END
                                    WHEN ABS(n.zret60) >= ABS(n.zret90) THEN
                                        CASE WHEN n.zret60 > 0 THEN 'UP' WHEN n.zret60 < 0 THEN 'DOWN' ELSE 'FLAT' END
                                    ELSE
                                        CASE WHEN n.zret90 > 0 THEN 'UP' WHEN n.zret90 < 0 THEN 'DOWN' ELSE 'FLAT' END
                                END
                            ELSE
                                CASE
                                    WHEN LEAST(ABS(n.zret30), ABS(n.zret60)) >= LEAST(ABS(n.zret60), ABS(n.zret90)) THEN
                                        CASE
                                            WHEN (CASE WHEN n.zret30 > 0 THEN 1 WHEN n.zret30 < 0 THEN -1 ELSE 0 END)
                                               = (CASE WHEN n.zret60 > 0 THEN 1 WHEN n.zret60 < 0 THEN -1 ELSE 0 END)
                                                THEN CASE WHEN n.zret60 > 0 THEN 'UP' WHEN n.zret60 < 0 THEN 'DOWN' ELSE 'FLAT' END
                                            ELSE 'MIXED'
                                        END
                                    ELSE
                                        CASE
                                            WHEN (CASE WHEN n.zret60 > 0 THEN 1 WHEN n.zret60 < 0 THEN -1 ELSE 0 END)
                                               = (CASE WHEN n.zret90 > 0 THEN 1 WHEN n.zret90 < 0 THEN -1 ELSE 0 END)
                                                THEN CASE WHEN n.zret60 > 0 THEN 'UP' WHEN n.zret60 < 0 THEN 'DOWN' ELSE 'FLAT' END
                                            ELSE 'MIXED'
                                        END
                                END
                        END AS direction
                    FROM now_scores n
                    LEFT JOIN prev_scores p
                      ON p.venue_id = n.venue_id
                     AND p.instrument_id = n.instrument_id
                    JOIN core.venue_symbols vs
                      ON vs.venue_id = n.venue_id
                     AND vs.instrument_id = n.instrument_id
                    JOIN core.venues v
                      ON v.venue_id = n.venue_id
                    JOIN core.instruments i
                      ON i.instrument_id = n.instrument_id
                    WHERE vs.is_active = true
                      AND v.is_active = true
                      AND i.is_active = true
                )
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY
                            (CASE
                                WHEN s.final_score >= 5.0 THEN 3
                                WHEN s.final_score >= 3.0 THEN 2
                                WHEN s.final_score >= 2.0 THEN 1
                                ELSE 0
                            END) DESC,
                            s.metric_now DESC NULLS LAST,
                            ABS(s.metric_now - s.metric_prev) DESC NULLS LAST,
                            s.venue_id ASC,
                            s.instrument_id ASC
                    ) AS rank,
                    s.venue_id,
                    s.instrument_id,
                    s.symbol,
                    s.ts,
                    CASE
                        WHEN s.final_score IS NULL THEN 'NORMAL'
                        WHEN s.final_score >= 5.0 THEN 'SEVERE'
                        WHEN s.final_score >= 3.0 THEN 'ANOMALY'
                        WHEN s.final_score >= 2.0 THEN 'WATCH'
                        ELSE 'NORMAL'
                    END AS final_level,
                    s.final_score,
                    CASE
                        WHEN s.ret_max >= s.vol_max AND s.ret_max >= s.rng_max THEN 'RET'
                        WHEN s.vol_max >= s.rng_max THEN 'VOL'
                        ELSE 'RNG'
                    END AS driver,
                    s.metric_now AS metric_value,
                    (s.metric_now - s.metric_prev) AS delta,
                    s.direction
                FROM scored s
                WHERE (:minSeveritySentinel = 1 OR
                      (CASE
                           WHEN s.final_score >= 5.0 THEN 3
                           WHEN s.final_score >= 3.0 THEN 2
                           WHEN s.final_score >= 2.0 THEN 1
                           ELSE 0
                       END) >= :minSeverityVal)
                  AND (:driverFilterSentinel = 1 OR
                       (CASE
                            WHEN s.ret_max >= s.vol_max AND s.ret_max >= s.rng_max THEN 'RET'
                            WHEN s.vol_max >= s.rng_max THEN 'VOL'
                            ELSE 'RNG'
                        END) = :driverFilterVal)
                  AND (:minDeltaAbsSentinel = 1 OR (s.metric_now IS NOT NULL AND s.metric_prev IS NOT NULL AND ABS(s.metric_now - s.metric_prev) >= :minDeltaAbsVal))
                ORDER BY
                    (CASE
                        WHEN s.final_score >= 5.0 THEN 3
                        WHEN s.final_score >= 3.0 THEN 2
                        WHEN s.final_score >= 2.0 THEN 1
                        ELSE 0
                    END) DESC,
                    s.metric_now DESC NULLS LAST,
                    ABS(s.metric_now - s.metric_prev) DESC NULLS LAST,
                    s.venue_id ASC,
                    s.instrument_id ASC
                LIMIT :limit
                """;

        // PostgreSQL: null 파라미터는 타입 추론 실패 → sentinel 사용
        int minSeveritySentinel = (minSeverity == null) ? 1 : 0;
        int minSeverityVal = (minSeverity == null) ? 0 : minSeverity;
        int driverFilterSentinel = (driverFilter == null || driverFilter.isEmpty()) ? 1 : 0;
        String driverFilterVal = (driverFilter == null || driverFilter.isEmpty()) ? "" : driverFilter;
        int minDeltaAbsSentinel = (minDeltaAbs == null || minDeltaAbs <= 0) ? 1 : 0;
        double minDeltaAbsVal = (minDeltaAbs == null || minDeltaAbs <= 0) ? 0.0 : minDeltaAbs;

        Query q = em.createNativeQuery(sql)
                .setParameter("timeframe", timeframe)
                .setParameter("scoreVersion", scoreVersion)
                .setParameter("mode", m)
                .setParameter("tab", t)
                .setParameter("prevRn", prevRn)
                .setParameter("limit", limit)
                .setParameter("minSeveritySentinel", minSeveritySentinel)
                .setParameter("minSeverityVal", minSeverityVal)
                .setParameter("driverFilterSentinel", driverFilterSentinel)
                .setParameter("driverFilterVal", driverFilterVal)
                .setParameter("minDeltaAbsSentinel", minDeltaAbsSentinel)
                .setParameter("minDeltaAbsVal", minDeltaAbsVal);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<TopRow> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            int idx = 0;
            int rank = ((Number) r[idx++]).intValue();
            long venueId = ((Number) r[idx++]).longValue();
            long instrumentId = ((Number) r[idx++]).longValue();
            String symbol = r[idx++] == null ? null : r[idx - 1].toString();
            OffsetDateTime ts = toOffsetDateTime(r[idx++]);
            String finalLevel = r[idx++] == null ? null : r[idx - 1].toString();
            Double finalScore = r[idx] == null ? null : ((Number) r[idx]).doubleValue();
            idx++;
            String driver = r[idx++] == null ? null : r[idx - 1].toString();
            Double metricValue = r[idx] == null ? null : ((Number) r[idx]).doubleValue();
            idx++;
            Double delta = r[idx] == null ? null : ((Number) r[idx]).doubleValue();
            idx++;
            String direction = r[idx] == null ? null : r[idx].toString();

            result.add(new TopRow(rank, venueId, instrumentId, symbol, ts, finalLevel, finalScore, driver, metricValue, delta, direction));
        }
        return result;
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {

        if (value == null) return null;

        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof Instant instant) return instant.atOffset(ZoneOffset.UTC);
        if (value instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);

        throw new IllegalArgumentException("Unsupported timestamp type: " + value.getClass());
    }
}

