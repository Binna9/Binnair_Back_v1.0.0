package com.bin.anomaly.score.service;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.repository.AnomalyScoreSeriesDao;
import com.bin.web.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnomalyScoreSeriesService {

    private final AnomalyScoreSeriesDao anomalyScoreSeriesDao;
    private final AnomalyScoreProperties props;

    private static final List<Integer> DEFAULT_WINDOWS = List.of(30, 60, 90);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public AnomalyScoreSeriesResponse getSeries(
            long venueId,
            long instrumentId,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive,
            String timeframe,
            String scoreVersion
    ) {
        if (fromInclusive == null || toInclusive == null) {
            throw new IllegalArgumentException("error.anomaly.series.invalid_range");
        }
        if (fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("error.anomaly.series.invalid_range");
        }

        String tf = (timeframe == null || timeframe.isBlank()) ? props.getTimeframe() : timeframe;
        String sv = (scoreVersion == null || scoreVersion.isBlank()) ? props.getScoreVersion() : scoreVersion;

        AnomalyScoreSeriesDao.SeriesMeta meta;
        try {
            meta = anomalyScoreSeriesDao.loadMeta(venueId, instrumentId);
        } catch (Exception e) {
            throw new NotFoundException("error.anomaly.series.notfound");
        }

        List<AnomalyScoreSeriesDao.SeriesRowMultiWindow> rows = anomalyScoreSeriesDao.loadSeriesMultiWindow(
                venueId,
                instrumentId,
                tf,
                sv,
                fromInclusive,
                toInclusive
        );

        List<AnomalyScoreSeriesResponse.Point> points = new ArrayList<>(rows.size());

        OffsetDateTime latestTs = null;
        Map<String, Double> latestScores = new LinkedHashMap<>();
        Map<String, Double> maxScores = new LinkedHashMap<>();
        Map<String, OffsetDateTime> maxScoreTs = new LinkedHashMap<>();

        initWindowMaps(latestScores, maxScores, maxScoreTs);

        for (AnomalyScoreSeriesDao.SeriesRowMultiWindow r : rows) {
            OffsetDateTime tsKst = toKst(r.ts());

            Map<String, Double> scores = new LinkedHashMap<>();
            Map<String, String> drivers = new LinkedHashMap<>();
            Map<String, AnomalyScoreSeriesResponse.Z> z = new LinkedHashMap<>();

            fillOneWindow(scores, drivers, z, "30", r.score30(), r.zRet30(), r.zVol30(), r.zRng30());
            fillOneWindow(scores, drivers, z, "60", r.score60(), r.zRet60(), r.zVol60(), r.zRng60());
            fillOneWindow(scores, drivers, z, "90", r.score90(), r.zRet90(), r.zVol90(), r.zRng90());

            points.add(new AnomalyScoreSeriesResponse.Point(
                    tsKst,
                    r.open(),
                    r.high(),
                    r.low(),
                    r.close(),
                    r.volume(),
                    scores,
                    drivers,
                    z
            ));

            latestTs = tsKst;
            latestScores.put("30", r.score30());
            latestScores.put("60", r.score60());
            latestScores.put("90", r.score90());

            updateMax("30", r.score30(), tsKst, maxScores, maxScoreTs);
            updateMax("60", r.score60(), tsKst, maxScores, maxScoreTs);
            updateMax("90", r.score90(), tsKst, maxScores, maxScoreTs);
        }

        AnomalyScoreSeriesResponse.Meta responseMeta = new AnomalyScoreSeriesResponse.Meta(
                venueId,
                meta.venueCode(),
                instrumentId,
                meta.instrumentSymbol(),
                meta.venueSymbol(),
                tf,
                sv,
                DEFAULT_WINDOWS,
                toKst(fromInclusive),
                toKst(toInclusive),
                OffsetDateTime.now(ZoneOffset.UTC).atZoneSameInstant(KST).toOffsetDateTime(),
                points.size()
        );

        AnomalyScoreSeriesResponse.Summary summary = new AnomalyScoreSeriesResponse.Summary(
                latestTs,
                latestScores,
                maxScores,
                maxScoreTs
        );

        return new AnomalyScoreSeriesResponse(responseMeta, summary, points);
    }

    private static OffsetDateTime toKst(OffsetDateTime ts) {
        if (ts == null) return null;
        return ts.atZoneSameInstant(KST).toOffsetDateTime();
    }

    private static void initWindowMaps(
            Map<String, Double> latestScores,
            Map<String, Double> maxScores,
            Map<String, OffsetDateTime> maxScoreTs
    ) {
        // 키를 항상 고정(30/60/90)으로 내려주기 위해 미리 초기화
        latestScores.put("30", null);
        latestScores.put("60", null);
        latestScores.put("90", null);

        maxScores.put("30", null);
        maxScores.put("60", null);
        maxScores.put("90", null);

        maxScoreTs.put("30", null);
        maxScoreTs.put("60", null);
        maxScoreTs.put("90", null);
    }

    private static void fillOneWindow(
            Map<String, Double> scores,
            Map<String, String> drivers,
            Map<String, AnomalyScoreSeriesResponse.Z> z,
            String key,
            Double score,
            Double zRet,
            Double zVol,
            Double zRng
    ) {
        scores.put(key, score);
        drivers.put(key, driverOf(zRet, zVol, zRng));
        z.put(key, new AnomalyScoreSeriesResponse.Z(zRet, zVol, zRng));
    }

    private static void updateMax(
            String key,
            Double score,
            OffsetDateTime ts,
            Map<String, Double> maxScores,
            Map<String, OffsetDateTime> maxScoreTs
    ) {
        if (score == null) return;
        Double cur = maxScores.get(key);
        if (cur == null || score > cur) {
            maxScores.put(key, score);
            maxScoreTs.put(key, ts);
        }
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

