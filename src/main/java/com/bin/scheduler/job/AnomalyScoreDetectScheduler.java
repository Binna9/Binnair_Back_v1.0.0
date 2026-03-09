package com.bin.scheduler.job;

import com.bin.anomaly.score.service.AnomalyScoreDetectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 운영에서는 외부 배치(크론/쿠버네티스 잡)로 호출하는 경우가 많아서 기본은 비활성.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "anomaly.score.detect.schedule.enabled", havingValue = "true")
public class AnomalyScoreDetectScheduler {

    private final AnomalyScoreDetectService anomalyScoreDetectService;

    @Scheduled(cron = "${anomaly.score.detect.schedule.cron:0 */5 * * * *}")
    public void run() {
        anomalyScoreDetectService.detectAllActive();
        log.info("[scheduler] AnomalyScoreDetectScheduler bean created");
    }
}

