package com.bin.anomaly.score.controller;

import com.bin.anomaly.score.model.AnomalyScoreFinalResponse;
import com.bin.anomaly.score.model.AnomalyScoreSeriesResponse;
import com.bin.anomaly.score.model.AnomalyScoreTopResponse;
import com.bin.anomaly.score.service.AnomalyScoreFinalService;
import com.bin.anomaly.score.service.AnomalyScoreScannerService;
import com.bin.anomaly.score.service.AnomalyScoreSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/anomaly/scores")
public class AnomalyScoreController {

    private final AnomalyScoreSeriesService anomalyScoreSeriesService;
    private final AnomalyScoreFinalService anomalyScoreFinalService;
    private final AnomalyScoreScannerService anomalyScoreScannerService;

    /**
     * 차트용 시계열 조회 API (Redis 스냅샷).
     * `from`, `to`는 ISO-8601 날짜 시간 문자열 로 전달 해야 합니다. ex) `2026-02-06T00:00:00Z`, `2026-02-06T00:00:00+09:00`
     */
    @GetMapping("/{venueId}/{instrumentId}/series")
    @Operation(summary = "단일 자산 캔들(OHLCV) + anomaly score 시계열 조회 (windowDays=30/60/90 동시)")
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

    @GetMapping("/top")
    @Operation(summary = "지금 가장 이상한 종목 Top N 조회 (프리미엄 스캐너/알림용)")
    public ResponseEntity<AnomalyScoreTopResponse> top(
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false, defaultValue = "consensus") String mode,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false, defaultValue = "12") Integer deltaBars
    ) {
        return ResponseEntity.ok(
                anomalyScoreScannerService.top(timeframe, mode, limit, deltaBars)
        );
    }

    @GetMapping("/top/vol")
    @Operation(summary = "거래량 이상 Top N (z_vol 기반 정렬)")
    public ResponseEntity<AnomalyScoreTopResponse> topVol(
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false, defaultValue = "consensus") String mode,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false, defaultValue = "12") Integer deltaBars,
            @RequestParam(required = false) String minLevel,
            @RequestParam(required = false) String driver,
            @RequestParam(required = false) Double minDeltaAbs
    ) {
        return ResponseEntity.ok(
                anomalyScoreScannerService.topVol(timeframe, mode, limit, deltaBars, minLevel, driver, minDeltaAbs)
        );
    }

    @GetMapping("/top/rng")
    @Operation(summary = "변동폭 이상 Top N (z_rng 기반 정렬)")
    public ResponseEntity<AnomalyScoreTopResponse> topRng(
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false, defaultValue = "consensus") String mode,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false, defaultValue = "12") Integer deltaBars,
            @RequestParam(required = false) String minLevel,
            @RequestParam(required = false) String driver,
            @RequestParam(required = false) Double minDeltaAbs
    ) {
        return ResponseEntity.ok(
                anomalyScoreScannerService.topRng(timeframe, mode, limit, deltaBars, minLevel, driver, minDeltaAbs)
        );
    }

    @GetMapping("/top/ret")
    @Operation(summary = "급등/급락 Top N (|z_ret| 기반 정렬 + direction)")
    public ResponseEntity<AnomalyScoreTopResponse> topRet(
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false, defaultValue = "consensus") String mode,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false, defaultValue = "12") Integer deltaBars,
            @RequestParam(required = false) String minLevel,
            @RequestParam(required = false) String driver,
            @RequestParam(required = false) Double minDeltaAbs
    ) {
        return ResponseEntity.ok(
                anomalyScoreScannerService.topRet(timeframe, mode, limit, deltaBars, minLevel, driver, minDeltaAbs)
        );
    }

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

    /**
     * @deprecated 실시간 Writer로 대체됨. 호출 시 410 Gone.
     */
    @Deprecated
    @PostMapping("/detect")
    @Operation(summary = "[제거됨] Anomaly score detect — 실시간 Writer 사용")
    public ResponseEntity<Map<String, String>> detectGone() {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("message", "error.anomaly.detect.gone"));
    }
}
