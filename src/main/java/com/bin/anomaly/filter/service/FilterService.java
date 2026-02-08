package com.bin.anomaly.filter.service;

import com.bin.anomaly.filter.model.InstrumentResponse;
import com.bin.anomaly.filter.model.VenueResponse;
import com.bin.anomaly.filter.repository.FilterDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 필터용 거래소/종목 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class FilterService {

    private final FilterDao filterDao;

    /**
     * 활성화 된 거래소 목록 조회
     * @param venueType 거래소 타입 필터 (선택: exchange, broker, data_vendor)
     * @return 거래소 목록
     */
    public List<VenueResponse> getVenues(String venueType) {
        return filterDao.listActiveVenues(venueType);
    }

    /**
     * 활성화 된 종목 목록 조회
     * @param assetClass 자산군 필터 (선택: crypto, equity, fx, index, commodity, rates, etf)
     * @param symbol 심볼 검색 (선택, 부분 일치)
     * @return 종목 목록
     */
    public List<InstrumentResponse> getInstruments(String assetClass, String symbol) {
        return filterDao.listActiveInstruments(assetClass, symbol);
    }
}

