package com.bin.anomaly.score.service;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.repository.AnomalyScoreSeriesDao;
import com.bin.web.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnomalyScoreSeriesService {

    private final AnomalyScoreSeriesDao anomalyScoreSeriesDao;
    private final AnomalyScoreProperties props;

    public AnomalyScoreSeriesResponse getSeries(
            long venueId,
            long instrumentId,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive,
            String timeframe,
            String scoreVersion,
            Integer windowDays
    ) {
        if (fromInclusive == null || toInclusive == null) {
            throw new IllegalArgumentException("error.anomaly.series.invalid_range");
        }
        if (fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("error.anomaly.series.invalid_range");
        }

        String tf = (timeframe == null || timeframe.isBlank()) ? props.getTimeframe() : timeframe;
        String sv = (scoreVersion == null || scoreVersion.isBlank()) ? props.getScoreVersion() : scoreVersion;
        int wd = (windowDays == null || windowDays <= 0) ? props.getWindowDays() : windowDays;

        AnomalyScoreSeriesDao.SeriesMeta meta;
        try {
            meta = anomalyScoreSeriesDao.loadMeta(venueId, instrumentId);
        } catch (Exception e) {
            throw new NotFoundException("error.anomaly.series.notfound");
        }

        List<AnomalyScoreSeriesDao.SeriesRow> rows = anomalyScoreSeriesDao.loadSeries(
                venueId,
                instrumentId,
                tf,
                sv,
                wd,
                fromInclusive,
                toInclusive
        );

        List<AnomalyScoreSeriesResponse.Point> points = new ArrayList<>(rows.size());

        Double maxScore = null;
        OffsetDateTime maxScoreTs = null;

        OffsetDateTime latestTs = null;
        Double latestScore = null;

        Integer detectedWindowDays = null;

        for (AnomalyScoreSeriesDao.SeriesRow r : rows) {
            String driver = driverOf(r.zRet(), r.zVol(), r.zRng());
            points.add(new AnomalyScoreSeriesResponse.Point(
                    r.ts(),
                    r.open(),
                    r.high(),
                    r.low(),
                    r.close(),
                    r.volume(),
                    r.score(),
                    r.zRet(),
                    r.zVol(),
                    r.zRng(),
                    driver
            ));

            if (detectedWindowDays == null && r.windowDays() != null) {
                detectedWindowDays = r.windowDays();
            }

            latestTs = r.ts();
            latestScore = r.score();

            if (r.score() != null) {
                if (maxScore == null || r.score() > maxScore) {
                    maxScore = r.score();
                    maxScoreTs = r.ts();
                }
            }
        }

        Integer finalWindowDays = detectedWindowDays != null ? detectedWindowDays : wd;

        AnomalyScoreSeriesResponse.Meta responseMeta = new AnomalyScoreSeriesResponse.Meta(
                venueId,
                meta.venueCode(),
                instrumentId,
                meta.instrumentSymbol(),
                meta.venueSymbol(),
                tf,
                sv,
                finalWindowDays,
                fromInclusive,
                toInclusive,
                OffsetDateTime.now(ZoneOffset.UTC),
                points.size()
        );

        AnomalyScoreSeriesResponse.Summary summary = new AnomalyScoreSeriesResponse.Summary(
                latestTs,
                latestScore,
                maxScore,
                maxScoreTs
        );

        return new AnomalyScoreSeriesResponse(responseMeta, summary, points);
    }

    private static String driverOf(Double zRet, Double zVol, Double zRng) {
        double a = zRet == null ? -1.0 : Math.abs(zRet);
        double b = zVol == null ? -1.0 : Math.abs(zVol);
        double c = zRng == null ? -1.0 : Math.abs(zRng);

        if (a < 0 && b < 0 && c < 0) return null;
        if (a >= b && a >= c) return "ret";
        if (b >= a && b >= c) return "vol";
        return "rng";
    }
}

