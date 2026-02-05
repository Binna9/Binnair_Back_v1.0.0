package com.bin.anomaly.score.controller;

import com.bin.anomaly.score.model.AnomalyScoreDetectResult;
import com.bin.anomaly.score.service.AnomalyScoreDetectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/anomaly/scores")
public class AnomalyScoreDetectController {

    private final AnomalyScoreDetectService anomalyScoreDetectService;

    @PostMapping("/detect")
    @Operation(summary = "Anomaly score detect 실행(활성 venue/instrument 대상, 5m 고정)")
    public ResponseEntity<AnomalyScoreDetectResult> detect() {
        return ResponseEntity.ok(anomalyScoreDetectService.detectAllActive());
    }
}

