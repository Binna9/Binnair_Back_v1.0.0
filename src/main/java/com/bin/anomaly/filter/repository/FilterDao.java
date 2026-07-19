package com.bin.anomaly.filter.repository;

import com.bin.anomaly.filter.model.InstrumentResponse;
import com.bin.anomaly.filter.model.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 필터용 거래소/종목 조회 DAO — web.market_symbols 위임.
 */
@Repository
@RequiredArgsConstructor
public class FilterDao {

    private final MarketSymbolDao marketSymbolDao;

    public List<VenueResponse> listActiveVenues(String venueType) {
        return marketSymbolDao.listActiveVenues(venueType);
    }

    public List<InstrumentResponse> listActiveInstruments(String assetClass, String symbol) {
        return marketSymbolDao.listActiveInstruments(assetClass, symbol);
    }
}
