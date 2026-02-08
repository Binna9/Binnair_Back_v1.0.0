package com.bin.anomaly.score.repository;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
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
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class AnomalyScoreSeriesDao {

    @PersistenceContext
    private final EntityManager em;
    private final AnomalyScoreProperties props;

    public record SeriesMeta(
            String venueCode,
            String instrumentSymbol,
            String venueSymbol
    ) {}

    public record SeriesRow(
            OffsetDateTime ts,
            double open,
            double high,
            double low,
            double close,
            double volume,
            Integer windowDays,
            Double zRet,
            Double zVol,
            Double zRng,
            Double score
    ) {}

    public record SeriesRowMultiWindow(
            OffsetDateTime ts,
            double open,
            double high,
            double low,
            double close,
            double volume,
            Double zRet30,
            Double zVol30,
            Double zRng30,
            Double score30,
            Double zRet60,
            Double zVol60,
            Double zRng60,
            Double score60,
            Double zRet90,
            Double zVol90,
            Double zRng90,
            Double score90
    ) {}

    public SeriesMeta loadMeta(long venueId, long instrumentId) {
        String sql = """
                SELECT
                  v.venue_code,
                  i.symbol,
                  vs.venue_symbol
                FROM core.venue_symbols vs
                JOIN core.venues v
                  ON v.venue_id = vs.venue_id
                JOIN core.instruments i
                  ON i.instrument_id = vs.instrument_id
                WHERE vs.venue_id = ?1
                  AND vs.instrument_id = ?2
                  AND vs.is_active = true
                  AND v.is_active = true
                  AND i.is_active = true
                """;

        Object[] row = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, venueId)
                .setParameter(2, instrumentId)
                .getSingleResult();

        return new SeriesMeta(
                row[0] == null ? null : row[0].toString(),
                row[1] == null ? null : row[1].toString(),
                row[2] == null ? null : row[2].toString()
        );
    }

    public List<SeriesRow> loadSeries(
            long venueId,
            long instrumentId,
            String timeframe,
            String scoreVersion,
            int windowDays,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        String sql = """
                SELECT
                  c.ts        AS ts,
                  c.open      AS open,
                  c.high      AS high,
                  c.low       AS low,
                  c.close     AS close,
                  c.volume    AS volume,
                  s.window_days AS window_days,
                  s.z_ret     AS z_ret,
                  s.z_vol     AS z_vol,
                  s.z_rng     AS z_rng,
                  s.score     AS score
                FROM core.candles c
                JOIN core.venue_symbols vs
                  ON vs.venue_id = c.venue_id
                 AND vs.instrument_id = c.instrument_id
                JOIN core.venues v
                  ON v.venue_id = c.venue_id
                LEFT JOIN core.anomaly_scores s
                  ON s.venue_id = c.venue_id
                 AND s.instrument_id = c.instrument_id
                 AND s.timeframe = c.timeframe
                 AND s.ts = c.ts
                 AND s.score_version = :scoreVersion
                 AND s.window_days = :windowDays
                WHERE c.venue_id = :venueId
                  AND c.instrument_id = :instrumentId
                  AND c.timeframe = :timeframe
                  AND c.is_final = true
                  AND c.ts <= (now() - (:lagSeconds || ' seconds')::interval)
                  AND c.ts >= :fromTs
                  AND c.ts <= :toTs
                  AND vs.is_active = true
                  AND v.is_active = true
                ORDER BY c.ts ASC
                """;

        Query q = em.createNativeQuery(sql)
                .setParameter("venueId", venueId)
                .setParameter("instrumentId", instrumentId)
                .setParameter("timeframe", Objects.requireNonNull(timeframe))
                .setParameter("scoreVersion", Objects.requireNonNull(scoreVersion))
                .setParameter("windowDays", windowDays)
                .setParameter("lagSeconds", props.getFinalCandleSafetyLag().getSeconds())
                .setParameter("fromTs", Timestamp.from(Objects.requireNonNull(fromInclusive).toInstant()))
                .setParameter("toTs", Timestamp.from(Objects.requireNonNull(toInclusive).toInstant()));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<SeriesRow> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            OffsetDateTime ts = toOffsetDateTime(r[0]);
            double open = ((Number) r[1]).doubleValue();
            double high = ((Number) r[2]).doubleValue();
            double low = ((Number) r[3]).doubleValue();
            double close = ((Number) r[4]).doubleValue();
            double volume = ((Number) r[5]).doubleValue();
            Integer rowWindowDays = (r[6] == null) ? null : ((Number) r[6]).intValue();
            Double zRet = (r[7] == null) ? null : ((Number) r[7]).doubleValue();
            Double zVol = (r[8] == null) ? null : ((Number) r[8]).doubleValue();
            Double zRng = (r[9] == null) ? null : ((Number) r[9]).doubleValue();
            Double score = (r[10] == null) ? null : ((Number) r[10]).doubleValue();

            result.add(new SeriesRow(ts, open, high, low, close, volume, rowWindowDays, zRet, zVol, zRng, score));
        }
        return result;
    }

    public List<SeriesRowMultiWindow> loadSeriesMultiWindow(
            long venueId,
            long instrumentId,
            String timeframe,
            String scoreVersion,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        String sql = """
                SELECT
                  c.ts        AS ts,
                  c.open      AS open,
                  c.high      AS high,
                  c.low       AS low,
                  c.close     AS close,
                  c.volume    AS volume,

                  MAX(CASE WHEN s.window_days = 30 THEN s.z_ret END) AS z_ret_30,
                  MAX(CASE WHEN s.window_days = 30 THEN s.z_vol END) AS z_vol_30,
                  MAX(CASE WHEN s.window_days = 30 THEN s.z_rng END) AS z_rng_30,
                  MAX(CASE WHEN s.window_days = 30 THEN s.score END) AS score_30,

                  MAX(CASE WHEN s.window_days = 60 THEN s.z_ret END) AS z_ret_60,
                  MAX(CASE WHEN s.window_days = 60 THEN s.z_vol END) AS z_vol_60,
                  MAX(CASE WHEN s.window_days = 60 THEN s.z_rng END) AS z_rng_60,
                  MAX(CASE WHEN s.window_days = 60 THEN s.score END) AS score_60,

                  MAX(CASE WHEN s.window_days = 90 THEN s.z_ret END) AS z_ret_90,
                  MAX(CASE WHEN s.window_days = 90 THEN s.z_vol END) AS z_vol_90,
                  MAX(CASE WHEN s.window_days = 90 THEN s.z_rng END) AS z_rng_90,
                  MAX(CASE WHEN s.window_days = 90 THEN s.score END) AS score_90
                FROM core.candles c
                JOIN core.venue_symbols vs
                  ON vs.venue_id = c.venue_id
                 AND vs.instrument_id = c.instrument_id
                JOIN core.venues v
                  ON v.venue_id = c.venue_id
                LEFT JOIN core.anomaly_scores s
                  ON s.venue_id = c.venue_id
                 AND s.instrument_id = c.instrument_id
                 AND s.timeframe = c.timeframe
                 AND s.ts = c.ts
                 AND s.score_version = :scoreVersion
                 AND s.window_days IN (30, 60, 90)
                WHERE c.venue_id = :venueId
                  AND c.instrument_id = :instrumentId
                  AND c.timeframe = :timeframe
                  AND c.is_final = true
                  AND c.ts <= (now() - (:lagSeconds || ' seconds')::interval)
                  AND c.ts >= :fromTs
                  AND c.ts <= :toTs
                  AND vs.is_active = true
                  AND v.is_active = true
                GROUP BY
                  c.ts, c.open, c.high, c.low, c.close, c.volume
                ORDER BY c.ts ASC
                """;

        Query q = em.createNativeQuery(sql)
                .setParameter("venueId", venueId)
                .setParameter("instrumentId", instrumentId)
                .setParameter("timeframe", Objects.requireNonNull(timeframe))
                .setParameter("scoreVersion", Objects.requireNonNull(scoreVersion))
                .setParameter("lagSeconds", props.getFinalCandleSafetyLag().getSeconds())
                .setParameter("fromTs", Timestamp.from(Objects.requireNonNull(fromInclusive).toInstant()))
                .setParameter("toTs", Timestamp.from(Objects.requireNonNull(toInclusive).toInstant()));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<SeriesRowMultiWindow> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            OffsetDateTime ts = toOffsetDateTime(r[0]);
            double open = ((Number) r[1]).doubleValue();
            double high = ((Number) r[2]).doubleValue();
            double low = ((Number) r[3]).doubleValue();
            double close = ((Number) r[4]).doubleValue();
            double volume = ((Number) r[5]).doubleValue();

            Double zRet30 = (r[6] == null) ? null : ((Number) r[6]).doubleValue();
            Double zVol30 = (r[7] == null) ? null : ((Number) r[7]).doubleValue();
            Double zRng30 = (r[8] == null) ? null : ((Number) r[8]).doubleValue();
            Double score30 = (r[9] == null) ? null : ((Number) r[9]).doubleValue();

            Double zRet60 = (r[10] == null) ? null : ((Number) r[10]).doubleValue();
            Double zVol60 = (r[11] == null) ? null : ((Number) r[11]).doubleValue();
            Double zRng60 = (r[12] == null) ? null : ((Number) r[12]).doubleValue();
            Double score60 = (r[13] == null) ? null : ((Number) r[13]).doubleValue();

            Double zRet90 = (r[14] == null) ? null : ((Number) r[14]).doubleValue();
            Double zVol90 = (r[15] == null) ? null : ((Number) r[15]).doubleValue();
            Double zRng90 = (r[16] == null) ? null : ((Number) r[16]).doubleValue();
            Double score90 = (r[17] == null) ? null : ((Number) r[17]).doubleValue();

            result.add(new SeriesRowMultiWindow(
                    ts, open, high, low, close, volume,
                    zRet30, zVol30, zRng30, score30,
                    zRet60, zVol60, zRng60, score60,
                    zRet90, zVol90, zRng90, score90
            ));
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

