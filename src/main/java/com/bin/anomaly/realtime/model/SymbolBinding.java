package com.bin.anomaly.realtime.model;

/**
 * web.market_symbols → Writer 바인딩.
 * instrumentId = symbol_id, instrumentSymbol = display_symbol.
 */
public record SymbolBinding(
        long venueId,
        String venueCode,
        long instrumentId,
        String instrumentSymbol,
        String venueSymbol
) {
    public String assetKey() {
        return venueId + ":" + instrumentId;
    }
}
