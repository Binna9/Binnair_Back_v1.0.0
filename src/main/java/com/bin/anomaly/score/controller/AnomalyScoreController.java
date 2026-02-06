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
     * `core.candles`의 OHLCV(확정봉) + 동일 `ts`의 `core.anomaly_scores`(score/z_*)를 합쳐서 한 배열(`points`)로 내려줍니다.
     * `from`, `to`는 ISO-8601 날짜시간 문자열로 전달해야 합니다. ex) `2026-02-06T00:00:00Z`, `2026-02-06T00:00:00+09:00`
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
     * 이상점수 적재(배치) 실행 API
     * 활성화된 venue/instrument 조합을 대상으로 이상점수를 계산해 `core.anomaly_scores`에 upsert 합니다.
     * 기간/증분 기준, warm-up 정책, 안전 lag 등은 서버 설정(`anomaly.score.*`)과 서비스 로직을 따릅니다.
     * 응답은 전체 대상 수/성공/스킵/실패/부분처리 카운트 및 일부 상세를 포함합니다.
     */
    @PostMapping("/detect")
    @Operation(summary = "Anomaly score detect 실행(활성 venue/instrument 대상, 5m 고정)")
    public ResponseEntity<AnomalyScoreDetectResult> detect() {
        return ResponseEntity.ok(anomalyScoreDetectService.detectAllActive());
    }
}

