package com.bin.anomaly.filter.repository;

import com.bin.anomaly.filter.model.InstrumentResponse;
import com.bin.anomaly.filter.model.VenueResponse;
import com.bin.anomaly.realtime.model.SymbolBinding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * web.market_symbols 조회 DAO (anomaly Writer / filter).
 */
@Repository
@RequiredArgsConstructor
public class MarketSymbolDao {

    @PersistenceContext
    private final EntityManager em;

    /**
     * Writer용 active 심볼 바인딩.
     * instrumentId = symbol_id, instrumentSymbol = display_symbol.
     */
    public List<SymbolBinding> listActiveSymbolBindings(List<String> venueCodes) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  m.venue_id,
                  m.venue_code,
                  m.symbol_id,
                  m.display_symbol,
                  m.venue_symbol
                FROM web.market_symbols m
                WHERE m.is_active = true
                """);

        boolean filterVenues = venueCodes != null && !venueCodes.isEmpty();
        if (filterVenues) {
            sql.append(" AND LOWER(m.venue_code) IN (:venueCodes)");
        }
        sql.append(" ORDER BY m.venue_id, m.symbol_id");

        Query query = em.createNativeQuery(sql.toString());
        if (filterVenues) {
            List<String> normalized = venueCodes.stream()
                    .filter(Objects::nonNull)
                    .map(c -> c.trim().toLowerCase(Locale.ROOT))
                    .filter(c -> !c.isEmpty())
                    .toList();
            query.setParameter("venueCodes", normalized);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<SymbolBinding> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new SymbolBinding(
                    ((Number) r[0]).longValue(),
                    r[1] == null ? null : r[1].toString(),
                    ((Number) r[2]).longValue(),
                    r[3] == null ? null : r[3].toString(),
                    r[4] == null ? null : r[4].toString()
            ));
        }
        return result;
    }

    public List<VenueResponse> listActiveVenues(String venueType) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  m.venue_id,
                  m.venue_code,
                  MIN(m.venue_type) AS venue_type,
                  TRUE AS is_active,
                  MIN(m.create_datetime) AS create_datetime
                FROM web.market_symbols m
                WHERE m.is_active = true
                """);

        if (venueType != null && !venueType.isBlank()) {
            sql.append(" AND LOWER(m.venue_type) = LOWER(:venueType)");
        }
        sql.append(" GROUP BY m.venue_id, m.venue_code ORDER BY m.venue_code");

        Query query = em.createNativeQuery(sql.toString());
        if (venueType != null && !venueType.isBlank()) {
            query.setParameter("venueType", venueType.trim());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<VenueResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new VenueResponse(
                    ((Number) r[0]).longValue(),
                    r[1] == null ? null : r[1].toString(),
                    r[2] == null ? null : r[2].toString(),
                    "UTC",
                    toBoolean(r[3], true),
                    "{}",
                    toOffsetDateTime(r[4])
            ));
        }
        return result;
    }

    public List<InstrumentResponse> listActiveInstruments(String assetClass, String symbol) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  m.symbol_id,
                  m.display_symbol,
                  m.asset_class,
                  m.base_asset,
                  m.quote_asset,
                  m.is_active,
                  m.create_datetime,
                  m.modify_datetime
                FROM web.market_symbols m
                WHERE m.is_active = true
                """);

        if (assetClass != null && !assetClass.isBlank()) {
            sql.append(" AND LOWER(m.asset_class) = LOWER(:assetClass)");
        }
        if (symbol != null && !symbol.isBlank()) {
            sql.append(" AND (m.display_symbol ILIKE :symbol OR m.venue_symbol ILIKE :symbol)");
        }
        sql.append(" ORDER BY m.asset_class, m.display_symbol");

        Query query = em.createNativeQuery(sql.toString());
        if (assetClass != null && !assetClass.isBlank()) {
            query.setParameter("assetClass", assetClass.trim());
        }
        if (symbol != null && !symbol.isBlank()) {
            query.setParameter("symbol", "%" + symbol.trim() + "%");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<InstrumentResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new InstrumentResponse(
                    ((Number) r[0]).longValue(),
                    r[1] == null ? null : r[1].toString(),
                    r[2] == null ? null : r[2].toString(),
                    r[3] == null ? null : r[3].toString(),
                    r[4] == null ? null : r[4].toString(),
                    null,
                    null,
                    null,
                    null,
                    toBoolean(r[5], true),
                    "{}",
                    toOffsetDateTime(r[6]),
                    toOffsetDateTime(r[7])
                ));
        }
        return result;
    }

    private static boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean b) return b;
        return defaultValue;
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof java.time.Instant instant) return instant.atOffset(ZoneOffset.UTC);
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        return null;
    }
}
