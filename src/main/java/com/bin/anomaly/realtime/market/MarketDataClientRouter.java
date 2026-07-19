package com.bin.anomaly.realtime.market;

import com.bin.anomaly.realtime.model.MarketCandle;
import com.bin.anomaly.realtime.model.SymbolBinding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class MarketDataClientRouter {

    private final List<MarketDataClient> clients;

    public MarketDataClient requireClient(String venueCode) {
        return clients.stream()
                .filter(c -> c.supports(venueCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MarketDataClient for venueCode=" + venueCode));
    }

    public List<MarketCandle> fetchHistory(
            SymbolBinding binding,
            String timeframe,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return requireClient(binding.venueCode()).fetchHistory(binding, timeframe, from, to);
    }

    public MarketCandle fetchLatest(SymbolBinding binding, String timeframe) {
        return requireClient(binding.venueCode()).fetchLatest(binding, timeframe);
    }

    public void subscribeKlines(
            List<SymbolBinding> bindings,
            String timeframe,
            BiConsumer<SymbolBinding, MarketCandle> onCandle
    ) {
        // venue별로 그룹핑해 각 클라이언트에 위임
        bindings.stream()
                .map(SymbolBinding::venueCode)
                .distinct()
                .forEach(code -> {
                    try {
                        MarketDataClient client = requireClient(code);
                        List<SymbolBinding> group = bindings.stream()
                                .filter(b -> code.equalsIgnoreCase(b.venueCode()))
                                .toList();
                        client.subscribeKlines(group, timeframe, onCandle);
                    } catch (IllegalStateException ignored) {
                        // unsupported venue
                    }
                });
    }

    public void unsubscribeAll() {
        for (MarketDataClient client : clients) {
            client.unsubscribeAll();
        }
    }
}
