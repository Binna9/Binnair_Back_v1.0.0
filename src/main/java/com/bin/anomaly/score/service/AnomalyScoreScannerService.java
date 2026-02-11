package com.bin.anomaly.score.service;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreTopResponse;
import com.bin.anomaly.score.repository.AnomalyScoreScannerDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnomalyScoreScannerService {

    private final AnomalyScoreScannerDao anomalyScoreScannerDao;
    private final AnomalyScoreProperties props;

    public AnomalyScoreTopResponse top(
            String timeframe,
            String mode,
            Integer limit,
            Integer deltaBars
    ) {
        return topByTab("AGG", timeframe, mode, limit, deltaBars, null, null, null);
    }

    private static OffsetDateTime toKst(OffsetDateTime ts) {
        return (ts == null) ? null : ts.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
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
        String tf = (timeframe == null || timeframe.isBlank()) ? props.getTimeframe() : timeframe.trim();
        String m = (mode == null || mode.isBlank()) ? "consensus" : mode.trim().toLowerCase();
        String t = (tab == null || tab.isBlank()) ? "AGG" : tab.trim().toUpperCase();

        int lim = (limit == null) ? 20 : limit;
        int db = (deltaBars == null) ? 12 : deltaBars;
        if (lim <= 0) lim = 20;
        if (lim > 200) lim = 200;
        if (db < 1) db = 1;
        if (db > 5000) db = 5000;

        Integer minSeverity = parseMinSeverity(minLevel);
        String driverFilter = (driver == null || driver.isBlank()) ? null : driver.trim().toUpperCase();
        Double minDeltaAbsNorm = (minDeltaAbs == null || minDeltaAbs <= 0.0) ? null : minDeltaAbs;

        String scoreVersion = props.getScoreVersion();
        List<AnomalyScoreScannerDao.TopRow> rows = anomalyScoreScannerDao.listTop(
                t, tf, scoreVersion, m, lim, db, minSeverity, driverFilter, minDeltaAbsNorm
        );

        OffsetDateTime responseTs = rows.isEmpty() ? null : toKst(rows.get(0).ts());

        List<AnomalyScoreTopResponse.Item> items = new ArrayList<>(rows.size());
        for (AnomalyScoreScannerDao.TopRow r : rows) {
            items.add(new AnomalyScoreTopResponse.Item(
                    r.rank(),
                    r.venueId(),
                    r.instrumentId(),
                    r.symbol(),
                    toKst(r.ts()),
                    r.finalLevel(),
                    r.finalScore(),
                    r.driver(),
                    r.metricValue(),
                    r.delta(),
                    r.direction()
            ));
        }

        return new AnomalyScoreTopResponse(tf, m, t, lim, db, responseTs, items);
    }

    private static Integer parseMinSeverity(String minLevel) {
        if (minLevel == null || minLevel.isBlank()) return null;
        String l = minLevel.trim().toUpperCase();
        return switch (l) {
            case "SEVERE" -> 3;
            case "ANOMALY" -> 2;
            case "WATCH" -> 1;
            case "NORMAL" -> 0;
            default -> throw new IllegalArgumentException("minLevel must be NORMAL/WATCH/ANOMALY/SEVERE");
        };
    }
}

