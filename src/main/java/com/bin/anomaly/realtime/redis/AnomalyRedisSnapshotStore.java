package com.bin.anomaly.realtime.redis;

import com.bin.anomaly.realtime.config.AnomalyRealtimeProperties;
import com.bin.anomaly.score.model.AnomalyScoreFinalResponse;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.model.AnomalyScoreTopResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyRedisSnapshotStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AnomalyRealtimeProperties props;

    public void markWarmingUp(boolean warmingUp) {
        redisTemplate.opsForValue().set(AnomalyRedisKeys.WARMING_UP, warmingUp ? "1" : "0",
                props.getSnapshotTtl().toSeconds() + 60, TimeUnit.SECONDS);
        if (!warmingUp) {
            redisTemplate.opsForValue().set(AnomalyRedisKeys.READY, "1",
                    props.getSnapshotTtl().toSeconds() + 60, TimeUnit.SECONDS);
        } else {
            redisTemplate.delete(AnomalyRedisKeys.READY);
        }
    }

    public boolean isReady() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(AnomalyRedisKeys.READY));
    }

    public boolean isWarmingUp() {
        String v = redisTemplate.opsForValue().get(AnomalyRedisKeys.WARMING_UP);
        return "1".equals(v);
    }

    public Optional<OffsetDateTime> getUpdatedAt() {
        String v = redisTemplate.opsForValue().get(AnomalyRedisKeys.UPDATED_AT);
        if (v == null || v.isBlank()) return Optional.empty();
        try {
            return Optional.of(OffsetDateTime.parse(v));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void touchUpdatedAt() {
        String iso = OffsetDateTime.now(ZoneOffset.UTC).toString();
        redisTemplate.opsForValue().set(AnomalyRedisKeys.UPDATED_AT, iso,
                props.getSnapshotTtl().toSeconds() + 60, TimeUnit.SECONDS);
    }

    public void putSeries(long venueId, long instrumentId, String timeframe, AnomalyScoreSeriesResponse response) {
        setJson(AnomalyRedisKeys.series(venueId, instrumentId, timeframe), response, seriesTtl());
    }

    public Optional<AnomalyScoreSeriesResponse> getSeries(long venueId, long instrumentId, String timeframe) {
        return getJson(AnomalyRedisKeys.series(venueId, instrumentId, timeframe), AnomalyScoreSeriesResponse.class);
    }

    public void putSeriesTip(long venueId, long instrumentId, String timeframe, AnomalyScoreSeriesResponse response) {
        setJson(AnomalyRedisKeys.seriesTip(venueId, instrumentId, timeframe), response, props.getSnapshotTtl());
    }

    public Optional<AnomalyScoreSeriesResponse> getSeriesTip(long venueId, long instrumentId, String timeframe) {
        return getJson(AnomalyRedisKeys.seriesTip(venueId, instrumentId, timeframe), AnomalyScoreSeriesResponse.class);
    }

    public void putFinal(long venueId, long instrumentId, String timeframe, String mode, AnomalyScoreFinalResponse response) {
        setJson(AnomalyRedisKeys.finals(venueId, instrumentId, timeframe, mode), response, props.getSnapshotTtl());
    }

    public Optional<AnomalyScoreFinalResponse> getFinal(long venueId, long instrumentId, String timeframe, String mode) {
        return getJson(AnomalyRedisKeys.finals(venueId, instrumentId, timeframe, mode), AnomalyScoreFinalResponse.class);
    }

    public void putTop(String tab, String timeframe, String mode, AnomalyScoreTopResponse response) {
        setJson(AnomalyRedisKeys.top(tab, timeframe, mode), response, props.getSnapshotTtl());
    }

    public Optional<AnomalyScoreTopResponse> getTop(String tab, String timeframe, String mode) {
        return getJson(AnomalyRedisKeys.top(tab, timeframe, mode), AnomalyScoreTopResponse.class);
    }

    public boolean tryAcquireWriterLock() {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                props.getWriterLockKey(),
                "1",
                props.getWriterLockTtl()
        );
        return Boolean.TRUE.equals(ok);
    }

    public void renewWriterLock() {
        redisTemplate.expire(props.getWriterLockKey(), props.getWriterLockTtl());
    }

    /** 확정 series는 publish 주기가 길어 TTL을 더 넉넉히. */
    private Duration seriesTtl() {
        long sec = Math.max(props.getSnapshotTtl().toSeconds(),
                props.getSeriesPublishIntervalMs() / 1000L * 3L + 30L);
        return Duration.ofSeconds(sec);
    }

    private void setJson(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("[anomaly-redis] serialize failed key={}: {}", key, e.getMessage());
        }
    }

    private <T> Optional<T> getJson(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("[anomaly-redis] deserialize failed key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }
}
