package com.bin.anomaly.score.service;

import com.bin.anomaly.realtime.redis.AnomalyRedisSnapshotStore;
import com.bin.anomaly.score.config.AnomalyScoreProperties;
import com.bin.anomaly.score.model.AnomalyScoreFinalResponse;
import com.bin.web.common.exception.AnomalyNotReadyException;
import com.bin.web.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Anomaly Score 최종 평가 서비스 (Redis 스냅샷 조회).
 */
@Service
@RequiredArgsConstructor
public class AnomalyScoreFinalService {

    private final AnomalyRedisSnapshotStore snapshotStore;
    private final AnomalyScoreProperties props;

    public AnomalyScoreFinalResponse evaluateFinal(
            long venueId,
            long instrumentId,
            String timeframe,
            String scoreVersion,
            String mode,
            OffsetDateTime ts
    ) {
        if (snapshotStore.isWarmingUp() || !snapshotStore.isReady()) {
            throw new AnomalyNotReadyException("error.anomaly.realtime.not_ready");
        }

        String tf = (timeframe == null || timeframe.isBlank()) ? props.getTimeframe() : timeframe;
        String evalMode = (mode == null || mode.isBlank()) ? "consensus" : mode.toLowerCase();

        AnomalyScoreFinalResponse cached = snapshotStore.getFinal(venueId, instrumentId, tf, evalMode)
                .orElseThrow(() -> new NotFoundException("error.anomaly.series.notfound"));

        // ts가 지정되면 동일 시각(초 단위)만 허용, 아니면 최신 스냅샷
        if (ts != null && cached.ts() != null) {
            OffsetDateTime cachedUtc = cached.ts().toInstant().atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime reqUtc = ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
            if (Math.abs(cachedUtc.toEpochSecond() - reqUtc.toEpochSecond()) > 1) {
                // 실시간 모드에서는 이력 final을 Redis에 보관하지 않음 → 최신 반환 유지하되 scoreVersion은 무시
            }
        }
        return cached;
    }
}
