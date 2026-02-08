package com.bin.anomaly.score.controller;

import com.bin.anomaly.score.model.AnomalyScoreDetectRequest;
import com.bin.anomaly.score.model.AnomalyScoreDetectResult;
import com.bin.anomaly.score.model.AnomalyScoreFinalResponse;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.service.AnomalyScoreDetectService;
import com.bin.anomaly.score.service.AnomalyScoreFinalService;
import com.bin.anomaly.score.service.AnomalyScoreSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/anomaly/scores")
public class AnomalyScoreController {

    private final AnomalyScoreDetectService anomalyScoreDetectService;
    private final AnomalyScoreSeriesService anomalyScoreSeriesService;
    private final AnomalyScoreFinalService anomalyScoreFinalService;

    /**
     * 차트용 시계열 조회 API
     * `from`, `to`는 ISO-8601 날짜 시간 문자열 로 전달 해야 합니다. ex) `2026-02-06T00:00:00Z`, `2026-02-06T00:00:00+09:00`
     */
    @GetMapping("/{venueId}/{instrumentId}/series")
    @Operation(summary = "단일 자산 캔들(OHLCV) + anomaly score 시계열 조회")
    public ResponseEntity<AnomalyScoreSeriesResponse> series(
            @PathVariable long venueId,
            @PathVariable long instrumentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String scoreVersion,
            @RequestParam(required = false) Integer windowDays
    ) {
        return ResponseEntity.ok(
                anomalyScoreSeriesService.getSeries(
                        venueId,
                        instrumentId,
                        from,
                        to,
                        timeframe,
                        scoreVersion,
                        windowDays
                )
        );
    }

    /**
     * 특정 venue/instrument 에 대한 이상 점수 적재 실행 API
     */
    @PostMapping("/detect")
    @Operation(summary = "특정 venue/instrument 에 대한 Anomaly score detect 실행")
    public ResponseEntity<AnomalyScoreDetectResult> detectOne(
            @RequestBody AnomalyScoreDetectRequest request
    ) {
        return ResponseEntity.ok(anomalyScoreDetectService.detectAllActive(request));
    }

    /**
     * 최종 평가 API
     * windowDays 30, 60, 90에 대한 데이터를 종합하여 최종 평가 수행
     * 
     * @param venueId 거래소 ID
     * @param instrumentId 종목 ID
     * @param timeframe 캔들 주기 (기본: 5m)
     * @param scoreVersion 점수 버전 (기본: z_v1)
     * @param mode 평가 모드 (max=OR, consensus=AND, 기본: consensus)
     * @param ts 시각 (optional, 없으면 최신 공통 ts 사용)
     * @return 최종 평가 결과
     */
    @GetMapping("/{venueId}/{instrumentId}/final")
    @Operation(summary = "최종 평가 API - windowDays 30, 60, 90 종합 평가")
    public ResponseEntity<AnomalyScoreFinalResponse> finalEvaluation(
            @PathVariable long venueId,
            @PathVariable long instrumentId,
            @RequestParam(required = false, defaultValue = "5m") String timeframe,
            @RequestParam(required = false, defaultValue = "z_v1") String scoreVersion,
            @RequestParam(required = false, defaultValue = "consensus") String mode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ts
    ) {
        return ResponseEntity.ok(
                anomalyScoreFinalService.evaluateFinal(
                        venueId,
                        instrumentId,
                        timeframe,
                        scoreVersion,
                        mode,
                        ts
                )
        );
    }
}

