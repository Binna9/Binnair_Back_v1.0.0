package com.bin.anomaly.score.repository;

import com.bin.anomaly.score.model.CandleRow;
import com.bin.anomaly.score.model.VenueInstrument;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
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

    /**
     * 특정 (venue_id, instrument_id, timeframe, score_version, window_days) 조합의
     * 마지막 점수 시각을 조회.
     * 
     * 중요: window_days도 점수 정의의 일부이므로 증분 기준에 반드시 포함되어야 함.
     * window_days가 다르면 완전히 다른 baseline으로 계산된 점수이므로 별도로 관리되어야 함.
     * 
     * @param venueId 거래소 ID
     * @param instrumentId 종목 ID
     * @param timeframe 캔들 주기 (예: "5m", "1h")
     * @param scoreVersion 점수 버전 (예: "z_v1")
     * @param windowDays baseline window 일수
     * @return 마지막 점수 시각 (없으면 null)
     */
    public OffsetDateTime findLastScoreTs(long venueId, long instrumentId, String timeframe, String scoreVersion, int windowDays) {

        String sql = """
                SELECT MAX(s.ts)
                FROM core.anomaly_scores s
                WHERE s.venue_id = ?1
                   AND s.instrument_id = ?2
                   AND s.timeframe = ?3
                   AND s.score_version = ?4
                   AND s.window_days = ?5
                """;
        Object value = em.createNativeQuery(sql)
                .setParameter(1, venueId)
                .setParameter(2, instrumentId)
                .setParameter(3, timeframe)
                .setParameter(4, scoreVersion)
                .setParameter(5, windowDays)
                .getSingleResult();
        return toOffsetDateTime(value);
    }

    /**
     * 특정 timeframe의 최종 확정 캔들 시각 조회.
     * 
     * @param venueId 거래소 ID
     * @param instrumentId 종목 ID
     * @param timeframe 캔들 주기 (예: "5m", "1h")
     * @param safetyLag 최신 봉 흔들림 방지 안전장치 (lag)
     * @return 최종 확정 캔들 시각 (없으면 null)
     */
    public OffsetDateTime findMaxFinalCandleTs(long venueId, long instrumentId, String timeframe, Duration safetyLag) {

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
        q.setParameter("timeframe", timeframe);
        q.setParameter("lagSeconds", safetyLag.getSeconds());

        Object value = q.getSingleResult();

        return toOffsetDateTime(value);
    }

    /**
     * 특정 timeframe의 확정 캔들 데이터 조회.
     * 
     * 안정된 상한(toInclusive)은 외부에서 주입되어야 함.
     * 예: findMaxFinalCandleTs()로 얻은 값은 이미 safetyLag가 적용된 안정된 값.
     * 
     * @param venueId 거래소 ID
     * @param instrumentId 종목 ID
     * @param timeframe 캔들 주기 (예: "5m", "1h")
     * @param fromInclusive 시작 시각 (포함)
     * @param toInclusive 종료 시각 (포함, 이미 안정된 값이어야 함)
     * @return 확정 캔들 리스트 (ts ASC 정렬)
     */
    public List<CandleRow> loadFinalCandles(long venueId, long instrumentId, String timeframe, 
                                             OffsetDateTime fromInclusive, OffsetDateTime toInclusive) {

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
                  AND c.ts >= :fromTs
                  AND c.ts <= :toTs
                  AND vs.is_active = true
                  AND v.is_active = true
                ORDER BY c.ts ASC
                """;

        Query q = em.createNativeQuery(sql)
                .setParameter("venueId", venueId)
                .setParameter("instrumentId", instrumentId)
                .setParameter("timeframe", timeframe)
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

