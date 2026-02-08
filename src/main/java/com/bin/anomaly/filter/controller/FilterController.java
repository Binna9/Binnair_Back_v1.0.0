package com.bin.anomaly.filter.controller;

import com.bin.anomaly.filter.model.InstrumentResponse;
import com.bin.anomaly.filter.model.VenueResponse;
import com.bin.anomaly.filter.service.FilterService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 필터용 거래소/종목 조회 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/anomaly/filter")
public class FilterController {

    private final FilterService filterService;

    /**
     * 활성화 된 거래소 목록 조회 API
     * @param venueType 거래소 타입 필터 (선택: exchange, broker, data_vendor)
     * @return 거래소 목록
     */
    @GetMapping("/venues")
    @Operation(summary = "활성화 된 거래소 목록 조회", description = "거래소 타입으로 필터링 가능")
    public ResponseEntity<List<VenueResponse>> getVenues(
            @RequestParam(required = false) String venueType
    ) {
        return ResponseEntity.ok(filterService.getVenues(venueType));
    }

    /**
     * 활성화 된 종목 목록 조회 API
     * @param assetClass 자산군 필터 (선택: crypto, equity, fx, index, commodity, rates, etf)
     * @param symbol 심볼 검색 (선택, 부분 일치)
     * @return 종목 목록
     */
    @GetMapping("/instruments")
    @Operation(summary = "활성화 된 종목 목록 조회", description = "자산군 및 심볼로 필터링 가능")
    public ResponseEntity<List<InstrumentResponse>> getInstruments(
            @RequestParam(required = false) String assetClass,
            @RequestParam(required = false) String symbol
    ) {
        return ResponseEntity.ok(filterService.getInstruments(assetClass, symbol));
    }
}

