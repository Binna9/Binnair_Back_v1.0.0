package com.bin.anomaly.score.repository;

import com.bin.anomaly.score.AnomalyScoreProperties;
import com.bin.anomaly.score.model.CandleRow;
import com.bin.anomaly.score.model.VenueInstrument;
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

/**
 * core.* (candles/venue_symbols/venues/anomaly_scores) 조회 전용 DAO.
 */
@Repository
@RequiredArgsConstructor
public class CoreMarketDataDao {

    @PersistenceContext
    private final EntityManager em;
    private final AnomalyScoreProperties props;

    public List<VenueInstrument> listActiveVenueInstruments() {

        String sql = """
                SELECT vs.venue_id, vs.instrument_id
                FROM core.venue_symbols vs
                JOIN core.venues v ON v.venue_id = vs.venue_id
                WHERE vs.is_active = true
                  AND v.is_active = true
                ORDER BY vs.venue_id, vs.instrument_id
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        List<VenueInstrument> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long venueId = ((Number) r[0]).longValue();
            long instrumentId = ((Number) r[1]).longValue();
            result.add(new VenueInstrument(venueId, instrumentId));
        }
        return result;
    }

    public OffsetDateTime findLastScoreTs(long venueId, long instrumentId) {

        String sql = """
                SELECT MAX(s.ts)
                FROM core.anomaly_scores s
                WHERE s.venue_id = ?1
                   AND s.instrument_id = ?2
                   AND s.timeframe = ?3
                   AND s.score_version = ?4
                """;
        Object value = em.createNativeQuery(sql)
                .setParameter(1, venueId)
                .setParameter(2, instrumentId)
                .setParameter(3, props.getTimeframe())
                .setParameter(4, props.getScoreVersion())
                .getSingleResult();
        return toOffsetDateTime(value);
    }

    public OffsetDateTime findMaxFinalCandleTs(long venueId, long instrumentId) {

        String sql = """
                SELECT MAX(c.ts)
                FROM core.candles c
                WHERE c.venue_id = :venueId
                  AND c.instrument_id = :instrumentId
                  AND c.timeframe = :timeframe
                  AND c.is_final = true
                  AND c.ts <= (now() - (:lagSeconds || ' seconds')::interval)
                """;
        Query q = em.createNativeQuery(sql);

        q.setParameter("venueId", venueId);
        q.setParameter("instrumentId", instrumentId);
        q.setParameter("timeframe", props.getTimeframe());
        q.setParameter("lagSeconds", props.getFinalCandleSafetyLag().getSeconds());

        Object value = q.getSingleResult();

        return toOffsetDateTime(value);
    }

    public List<CandleRow> loadFinalCandles(long venueId, long instrumentId, OffsetDateTime fromInclusive, OffsetDateTime toInclusive) {

        String sql = """
                SELECT
                    c.ts      AS ts,
                    c.high    AS high,
                    c.low     AS low,
                    c.close   AS close,
                    c.volume  AS volume
                FROM core.candles c
                JOIN core.venue_symbols vs
                  ON vs.venue_id = c.venue_id
                 AND vs.instrument_id = c.instrument_id
                JOIN core.venues v
                  ON v.venue_id = c.venue_id
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
                .setParameter("timeframe", props.getTimeframe())
                .setParameter("lagSeconds", props.getFinalCandleSafetyLag().getSeconds())
                .setParameter("fromTs", Timestamp.from(Objects.requireNonNull(fromInclusive).toInstant()))
                .setParameter("toTs", Timestamp.from(Objects.requireNonNull(toInclusive).toInstant()));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<CandleRow> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            OffsetDateTime ts = toOffsetDateTime(r[0]);
            double high = ((Number) r[1]).doubleValue();
            double low = ((Number) r[2]).doubleValue();
            double close = ((Number) r[3]).doubleValue();
            double volume = ((Number) r[4]).doubleValue();
            result.add(new CandleRow(ts, high, low, close, volume));
        }
        return result;
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {

        if (value == null) return null;

        if (value instanceof OffsetDateTime odt) return odt;

        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }

        if (value instanceof Timestamp ts) {
            return ts.toInstant().atOffset(ZoneOffset.UTC);
        }

        throw new IllegalArgumentException("Unsupported timestamp type: " + value.getClass());
    }
}

