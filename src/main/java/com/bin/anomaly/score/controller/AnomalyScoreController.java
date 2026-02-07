package com.bin.anomaly.score.controller;

import com.bin.anomaly.score.model.AnomalyScoreDetectResult;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.service.AnomalyScoreDetectService;
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
            @RequestParam(required = false) String scoreVersion
    ) {
        return ResponseEntity.ok(
                anomalyScoreSeriesService.getSeries(
                        venueId,
                        instrumentId,
                        from,
                        to,
                        timeframe,
                        scoreVersion
                )
        );
    }

    /**
     * 이상 점수 적재(배치) 실행 API
     */
    @PostMapping("/detect")
    @Operation(summary = "Anomaly score detect 실행(활성 venue/instrument 대상, 5m 고정)")
    public ResponseEntity<AnomalyScoreDetectResult> detect() {
        return ResponseEntity.ok(anomalyScoreDetectService.detectAllActive());
    }
}

