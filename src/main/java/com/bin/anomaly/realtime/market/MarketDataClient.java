package com.bin.anomaly.realtime.market;

import com.bin.anomaly.realtime.model.MarketCandle;
import com.bin.anomaly.realtime.model.SymbolBinding;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 거래소 시세 원천 클라이언트.
 */
public interface MarketDataClient {

    /**
     * 이 클라이언트가 해당 venue를 지원하는지.
     */
    boolean supports(String venueCode);

    /**
     * 과거 확정봉 조회 (워밍업).
     */
    List<MarketCandle> fetchHistory(
            SymbolBinding binding,
            String timeframe,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    );

    /**
     * 최신 봉(진행 중 포함) 1개. 미확정이면 isFinal=false.
     */
    MarketCandle fetchLatest(SymbolBinding binding, String timeframe);

    /**
     * 최근 N개 봉. 이미 마감된 봉은 isFinal=true, 진행 중 tip은 false.
     */
    default List<MarketCandle> fetchRecent(SymbolBinding binding, String timeframe, int limit) {
        MarketCandle latest = fetchLatest(binding, timeframe);
        return latest == null ? List.of() : List.of(latest);
    }

    /**
     * kline 스트림 구독. 콜백: (binding, candle).
     * REST-only 구현은 no-op일 수 있음.
     */
    default void subscribeKlines(
            List<SymbolBinding> bindings,
            String timeframe,
            BiConsumer<SymbolBinding, MarketCandle> onCandle
    ) {
        // optional
    }

    default void unsubscribeAll() {
        // optional
    }
}
