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
            Integer windowDays = (r[6] == null) ? null : ((Number) r[6]).intValue();
            Double zRet = (r[7] == null) ? null : ((Number) r[7]).doubleValue();
            Double zVol = (r[8] == null) ? null : ((Number) r[8]).doubleValue();
            Double zRng = (r[9] == null) ? null : ((Number) r[9]).doubleValue();
            Double score = (r[10] == null) ? null : ((Number) r[10]).doubleValue();

            result.add(new SeriesRow(ts, open, high, low, close, volume, windowDays, zRet, zVol, zRng, score));
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

