package com.bin.anomaly.filter.repository;

import com.bin.anomaly.filter.model.InstrumentResponse;
import com.bin.anomaly.filter.model.VenueResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 필터용 거래소/종목 조회 DAO
 */
@Repository
@RequiredArgsConstructor
public class FilterDao {

    @PersistenceContext
    private final EntityManager em;

    public List<VenueResponse> listActiveVenues(String venueType) {
        StringBuilder sql = new StringBuilder("""
                SELECT 
                    v.venue_id,
                    v.venue_code,
                    v.venue_type,
                    v.timezone,
                    v.is_active,
                    v.metadata,
                    v.create_datetime
                FROM core.venues v
                WHERE v.is_active = true
                """);

        if (venueType != null && !venueType.isBlank()) {
            sql.append(" AND v.venue_type = CAST(:venueType AS core.venue_type)");
        }

        sql.append(" ORDER BY v.venue_code");

        Query query = em.createNativeQuery(sql.toString());
        if (venueType != null && !venueType.isBlank()) {
            query.setParameter("venueType", venueType);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<VenueResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long venueId = ((Number) r[0]).longValue();
            String venueCode = (String) r[1];
            String venueTypeStr = (String) r[2];
            String timezone = (String) r[3];
            boolean isActive = (Boolean) r[4];
            String metadata = r[5] != null ? r[5].toString() : "{}";
            OffsetDateTime createDatetime = toOffsetDateTime(r[6]);

            result.add(new VenueResponse(
                    venueId,
                    venueCode,
                    venueTypeStr,
                    timezone,
                    isActive,
                    metadata,
                    createDatetime
            ));
        }
        return result;
    }

    public List<InstrumentResponse> listActiveInstruments(String assetClass, String symbol) {
        StringBuilder sql = new StringBuilder("""
                SELECT 
                    i.instrument_id,
                    i.symbol,
                    i.asset_class,
                    i.base_asset,
                    i.quote_asset,
                    i.currency,
                    i.country,
                    i.mic,
                    i.session_calendar,
                    i.is_active,
                    i.metadata,
                    i.create_datetime,
                    i.modify_datetime
                FROM core.instruments i
                WHERE i.is_active = true
                """);

        if (assetClass != null && !assetClass.isBlank()) {
            sql.append(" AND i.asset_class = CAST(:assetClass AS core.asset_class)");
        }

        if (symbol != null && !symbol.isBlank()) {
            sql.append(" AND i.symbol ILIKE :symbol");
        }

        sql.append(" ORDER BY i.asset_class, i.symbol");

        Query query = em.createNativeQuery(sql.toString());
        if (assetClass != null && !assetClass.isBlank()) {
            query.setParameter("assetClass", assetClass);
        }
        if (symbol != null && !symbol.isBlank()) {
            query.setParameter("symbol", "%" + symbol + "%");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<InstrumentResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long instrumentId = ((Number) r[0]).longValue();
            String symbolStr = (String) r[1];
            String assetClassStr = (String) r[2];
            String baseAsset = (String) r[3];
            String quoteAsset = (String) r[4];
            String currency = (String) r[5];
            String country = (String) r[6];
            String mic = (String) r[7];
            String sessionCalendar = (String) r[8];
            boolean isActive = (Boolean) r[9];
            String metadata = r[10] != null ? r[10].toString() : "{}";
            OffsetDateTime createDatetime = toOffsetDateTime(r[11]);
            OffsetDateTime modifyDatetime = toOffsetDateTime(r[12]);

            result.add(new InstrumentResponse(
                    instrumentId,
                    symbolStr,
                    assetClassStr,
                    baseAsset,
                    quoteAsset,
                    currency,
                    country,
                    mic,
                    sessionCalendar,
                    isActive,
                    metadata,
                    createDatetime,
                    modifyDatetime
            ));
        }
        return result;
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;

        if (value instanceof OffsetDateTime odt) return odt;

        if (value instanceof java.time.Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }

        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atOffset(ZoneOffset.UTC);
        }

        return null;
    }
}

