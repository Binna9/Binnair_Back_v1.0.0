package com.bin.anomaly.score.service;

import com.bin.anomaly.realtime.redis.AnomalyRedisSnapshotStore;
import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreTopResponse;
import com.bin.web.common.exception.AnomalyNotReadyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnomalyScoreScannerService {

    private final AnomalyRedisSnapshotStore snapshotStore;
    private final AnomalyScoreProperties props;

    public AnomalyScoreTopResponse top(
            String timeframe,
            String mode,
            Integer limit,
            Integer deltaBars
    ) {
        return topByTab("AGG", timeframe, mode, limit, deltaBars, null, null, null);
    }

    public AnomalyScoreTopResponse topVol(
            String timeframe,
            String mode,
            Integer limit,
            Integer deltaBars,
            String minLevel,
            String driver,
            Double minDeltaAbs
    ) {
        return topByTab("VOL", timeframe, mode, limit, deltaBars, minLevel, driver, minDeltaAbs);
    }

    public AnomalyScoreTopResponse topRng(
            String timeframe,
            String mode,
            Integer limit,
            Integer deltaBars,
            String minLevel,
            String driver,
            Double minDeltaAbs
    ) {
        return topByTab("RNG", timeframe, mode, limit, deltaBars, minLevel, driver, minDeltaAbs);
    }

    public AnomalyScoreTopResponse topRet(
            String timeframe,
            String mode,
            Integer limit,
            Integer deltaBars,
            String minLevel,
            String driver,
            Double minDeltaAbs
    ) {
        return topByTab("RET", timeframe, mode, limit, deltaBars, minLevel, driver, minDeltaAbs);
    }

    public AnomalyScoreTopResponse topByTab(
            String tab,
            String timeframe,
            String mode,
            Integer limit,
            Integer deltaBars,
            String minLevel,
            String driver,
            Double minDeltaAbs
    ) {
        if (snapshotStore.isWarmingUp() || !snapshotStore.isReady()) {
            throw new AnomalyNotReadyException("error.anomaly.realtime.not_ready");
        }

        String tf = (timeframe == null || timeframe.isBlank()) ? props.getTimeframe() : timeframe.trim();
        String m = (mode == null || mode.isBlank()) ? "consensus" : mode.trim().toLowerCase(Locale.ROOT);
        String t = (tab == null || tab.isBlank()) ? "AGG" : tab.trim().toUpperCase(Locale.ROOT);

        int limRaw = (limit == null) ? 20 : limit;
        int dbRaw = (deltaBars == null) ? 12 : deltaBars;
        final int lim = limRaw <= 0 ? 20 : Math.min(limRaw, 200);
        final int db = dbRaw < 1 ? 1 : Math.min(dbRaw, 5000);

        Integer minSeverity = parseMinSeverity(minLevel);
        String driverFilter = (driver == null || driver.isBlank()) ? null : driver.trim().toUpperCase(Locale.ROOT);
        Double minDeltaAbsNorm = (minDeltaAbs == null || minDeltaAbs <= 0.0) ? null : minDeltaAbs;

        AnomalyScoreTopResponse cached = snapshotStore.getTop(t, tf, m)
                .orElseGet(() -> new AnomalyScoreTopResponse(tf, m, t, lim, db, null, List.of()));

        List<AnomalyScoreTopResponse.Item> filtered = new ArrayList<>();
        int rank = 1;
        for (AnomalyScoreTopResponse.Item item : cached.items()) {
            if (minSeverity != null
                    && AnomalyScoreMath.severityRank(item.finalLevel()) < minSeverity) {
                continue;
            }
            if (driverFilter != null
                    && (item.driver() == null || !driverFilter.equalsIgnoreCase(item.driver()))) {
                continue;
            }
            if (minDeltaAbsNorm != null
                    && (item.delta() == null || Math.abs(item.delta()) < minDeltaAbsNorm)) {
                continue;
            }
            filtered.add(new AnomalyScoreTopResponse.Item(
                    rank++,
                    item.venueId(),
                    item.instrumentId(),
                    item.symbol(),
                    item.ts(),
                    item.finalLevel(),
                    item.finalScore(),
                    item.driver(),
                    item.metricValue(),
                    item.delta(),
                    item.direction()
            ));
            if (filtered.size() >= lim) break;
        }

        return new AnomalyScoreTopResponse(
                tf,
                m,
                t,
                lim,
                db,
                filtered.isEmpty() ? null : filtered.get(0).ts(),
                filtered
        );
    }

    private static Integer parseMinSeverity(String minLevel) {
        if (minLevel == null || minLevel.isBlank()) return null;
        String l = minLevel.trim().toUpperCase(Locale.ROOT);
        return switch (l) {
            case "SEVERE" -> 3;
            case "ANOMALY" -> 2;
            case "WATCH" -> 1;
            case "NORMAL" -> 0;
            default -> throw new IllegalArgumentException(
                    "minLevel must be NORMAL/WATCH/ANOMALY/SEVERE"
            );
        };
    }
}
