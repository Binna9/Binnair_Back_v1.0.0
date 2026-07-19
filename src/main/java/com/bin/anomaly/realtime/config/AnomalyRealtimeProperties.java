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

    /**
     * Writer 활성화 여부. false면 Redis 스냅샷을 갱신하지 않음.
     */
    private boolean enabled = true;

    /**
     * tip/top/final Redis 갱신 주기.
     */
    private Duration interval = Duration.ofSeconds(1);

    /**
     * @Scheduled fixedDelay용 ms. interval과 맞출 것.
     */
    private long intervalMs = 1000;

    /**
     * 지원 venue_code (소문자). 기본 binance만.
     */
    private List<String> venueCodes = new ArrayList<>(List.of("binance"));

    /**
     * Binance REST base URL.
     */
    private String binanceRestBaseUrl = "https://api.binance.com";

    /**
     * Binance combined stream WS URL (query 없이 host path까지).
     */
    private String binanceWsBaseUrl = "wss://stream.binance.com:9443/stream";

    /**
     * true면 WS 구독, false면 REST로 latest kline만 폴링.
     */
    private boolean useWebSocket = true;

    /**
     * 워밍업/보관할 series 일수 (차트용 Redis 적재 길이).
     */
    private int seriesRetentionDays = 30;

    /**
     * tip 샘플(미확정 궤적) 보관 기간. Writer interval마다 1포인트 append.
     * 예: 1h + interval 1s ≈ 3600점, 2s ≈ 1800점.
     */
    private Duration tipRetention = Duration.ofHours(1);

    /**
     * 롤링 baseline 확보용 히스토리 일수 (REST 워밍업).
     */
    private int warmupHistoryDays = 90;

    /**
     * 처리 자산 자산 수 (0이면 제한 없음).
     */
    private int maxAssets = 0;

    /**
     * Redis 스냅샷 TTL.
     */
    private Duration snapshotTtl = Duration.ofSeconds(10);

    /**
     * Top 스냅샷에 미리 계산해 둘 최대 limit.
     */
    private int topSnapshotLimit = 200;

    /**
     * Top delta 계산용 봉 수.
     */
    private int deltaBars = 12;

    /**
     * Writer 분산락 키 (멀티 인스턴스 시 단일 Writer).
     */
    private String writerLockKey = "anomaly:meta:writerLock";

    /**
     * Writer 락 TTL.
     */
    private Duration writerLockTtl = Duration.ofSeconds(5);
}
