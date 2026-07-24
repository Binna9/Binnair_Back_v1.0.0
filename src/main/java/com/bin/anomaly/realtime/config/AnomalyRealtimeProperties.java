package com.bin.anomaly.realtime.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Anomaly 실시간 Writer/Redis 스냅샷 설정.
 */
@Data
@ConfigurationProperties(prefix = "anomaly.realtime")
public class AnomalyRealtimeProperties {

    /** Writer 활성화 여부. false면 Redis 스냅샷을 갱신하지 않음. */
    private boolean enabled = true;

    /** tip/top/final Redis 갱신 주기 (Duration, 참고용). */
    private Duration interval = Duration.ofSeconds(2);

    /** tip/final/top @Scheduled fixedDelay(ms). */
    private long intervalMs = 2000;

    /** 확정봉 series Redis 갱신 주기(ms). tip보다 길게. */
    private long seriesPublishIntervalMs = 15000;

    /**
     * REST tip/확정 보정 주기(ms). WS 사용 중이면 이 주기로만 fetchRecent.
     * 확정봉 gap이 있으면 즉시 gap-fill.
     */
    private long restReconcileIntervalMs = 10000;

    /** 지원 venue_code (소문자). */
    private List<String> venueCodes = new ArrayList<>(List.of("binance"));

    private String binanceRestBaseUrl = "https://api.binance.com";

    private String binanceWsBaseUrl = "wss://stream.binance.com:9443/stream";

    /** true면 WS 구독 + REST 스로틀 보정, false면 REST만. */
    private boolean useWebSocket = true;

    /** 확정봉 series Redis 보관 일수. */
    private int seriesRetentionDays = 7;

    /** tip 샘플 보관. 예: 30m + interval 2s ≈ 900점. */
    private Duration tipRetention = Duration.ofMinutes(30);

    /** 롤링 baseline 워밍업 히스토리 일수. */
    private int warmupHistoryDays = 90;

    /** 처리 최대 자산 수 (0이면 제한 없음). */
    private int maxAssets = 0;

    /**
     * Redis 스냅샷 TTL. seriesPublishIntervalMs보다 길어야 확정 series 키가 안 끊김.
     */
    private Duration snapshotTtl = Duration.ofSeconds(60);

    /** Top 스냅샷 개수. */
    private int topSnapshotLimit = 5;

    /** Top delta 계산용 봉 수. */
    private int deltaBars = 12;

    /** publish할 final/top mode. 기본 consensus만. */
    private List<String> scoreModes = new ArrayList<>(List.of("consensus"));

    private String writerLockKey = "anomaly:meta:writerLock";

    private Duration writerLockTtl = Duration.ofSeconds(5);
}
