package com.bin.anomaly.score.service;

import com.bin.anomaly.realtime.redis.AnomalyRedisSnapshotStore;
import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.web.common.exception.AnomalyNotReadyException;
import com.bin.web.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    private final AnomalyRedisSnapshotStore snapshotStore;
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
            throw new com.bin.web.common.exception.IllegalArgumentException("error.anomaly.series.invalid_range");
        }
        if (fromInclusive.isAfter(toInclusive)) {
            throw new com.bin.web.common.exception.IllegalArgumentException("error.anomaly.series.invalid_range");
        }

        ensureReady();

        String tf = (timeframe == null || timeframe.isBlank()) ? props.getTimeframe() : timeframe;
        String sv = (scoreVersion == null || scoreVersion.isBlank()) ? props.getScoreVersion() : scoreVersion;

        AnomalyScoreSeriesResponse confirmed = snapshotStore.getSeries(venueId, instrumentId, tf)
                .orElse(null);
        AnomalyScoreSeriesResponse tips = snapshotStore.getSeriesTip(venueId, instrumentId, tf)
                .orElse(null);

        if (confirmed == null && tips == null) {
            throw new NotFoundException("error.anomaly.series.notfound");
        }

        List<AnomalyScoreSeriesResponse.Point> confirmedPts = filterRange(
                confirmed != null ? confirmed.points() : List.of(), fromInclusive, toInclusive);
        List<AnomalyScoreSeriesResponse.Point> tipPts = filterRange(
                tips != null ? tips.points() : List.of(), fromInclusive, toInclusive);

        // 하위호환: 예전 full series(확정+tip)만 있을 때 tip 키 없음
        List<AnomalyScoreSeriesResponse.Point> points = mergeByTs(confirmedPts, tipPts);

        OffsetDateTime latestTs = null;
        Map<String, Double> latestScores = new LinkedHashMap<>();
        Map<String, Double> maxScores = new LinkedHashMap<>();
        Map<String, OffsetDateTime> maxScoreTs = new LinkedHashMap<>();
        initWindowMaps(latestScores, maxScores, maxScoreTs);

        for (AnomalyScoreSeriesResponse.Point p : points) {
            latestTs = p.ts();
            latestScores.put("30", p.scores().get("30"));
            latestScores.put("60", p.scores().get("60"));
            latestScores.put("90", p.scores().get("90"));
            updateMax("30", p.scores().get("30"), p.ts(), maxScores, maxScoreTs);
            updateMax("60", p.scores().get("60"), p.ts(), maxScores, maxScoreTs);
            updateMax("90", p.scores().get("90"), p.ts(), maxScores, maxScoreTs);
        }

        AnomalyScoreSeriesResponse.Meta srcMeta = confirmed != null ? confirmed.meta()
                : (tips != null ? tips.meta() : null);

        AnomalyScoreSeriesResponse.Meta meta = new AnomalyScoreSeriesResponse.Meta(
                venueId,
                srcMeta != null ? srcMeta.venueCode() : null,
                instrumentId,
                srcMeta != null ? srcMeta.instrumentSymbol() : null,
                srcMeta != null ? srcMeta.venueSymbol() : null,
                tf,
                sv,
                DEFAULT_WINDOWS,
                toKst(fromInclusive),
                toKst(toInclusive),
                OffsetDateTime.now(ZoneOffset.UTC).atZoneSameInstant(KST).toOffsetDateTime(),
                points.size()
        );

        return new AnomalyScoreSeriesResponse(
                meta,
                new AnomalyScoreSeriesResponse.Summary(latestTs, latestScores, maxScores, maxScoreTs),
                points
        );
    }

    private static List<AnomalyScoreSeriesResponse.Point> mergeByTs(
            List<AnomalyScoreSeriesResponse.Point> confirmed,
            List<AnomalyScoreSeriesResponse.Point> tips
    ) {
        List<AnomalyScoreSeriesResponse.Point> out = new ArrayList<>(confirmed.size() + tips.size());
        int i = 0;
        int j = 0;
        while (i < confirmed.size() && j < tips.size()) {
            Instant a = confirmed.get(i).ts().toInstant();
            Instant b = tips.get(j).ts().toInstant();
            int cmp = a.compareTo(b);
            if (cmp < 0) {
                out.add(confirmed.get(i++));
            } else if (cmp > 0) {
                out.add(tips.get(j++));
            } else {
                out.add(tips.get(j++));
                i++;
            }
        }
        while (i < confirmed.size()) out.add(confirmed.get(i++));
        while (j < tips.size()) out.add(tips.get(j++));
        return out;
    }

    private static List<AnomalyScoreSeriesResponse.Point> filterRange(
            List<AnomalyScoreSeriesResponse.Point> points,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        List<AnomalyScoreSeriesResponse.Point> out = new ArrayList<>();
        for (AnomalyScoreSeriesResponse.Point p : points) {
            OffsetDateTime tsUtc = p.ts().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
            if (tsUtc.isBefore(fromInclusive) || tsUtc.isAfter(toInclusive)) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    private void ensureReady() {
        if (snapshotStore.isWarmingUp() || !snapshotStore.isReady()) {
            throw new AnomalyNotReadyException("error.anomaly.realtime.not_ready");
        }
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
}
