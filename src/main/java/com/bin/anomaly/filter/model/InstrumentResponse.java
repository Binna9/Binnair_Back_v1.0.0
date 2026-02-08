package com.bin.anomaly.filter.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 종목 정보 응답 모델
 */
@Schema(description = "종목 정보")
public record InstrumentResponse(
        @Schema(description = "종목 ID", example = "1")
        Long instrumentId,

        @Schema(description = "심볼", example = "BTC/USDT")
        String symbol,

        @Schema(description = "자산군", example = "crypto")
        String assetClass,

        @Schema(description = "기초자산", example = "BTC")
        String baseAsset,

        @Schema(description = "상대자산", example = "USDT")
        String quoteAsset,

        @Schema(description = "거래통화", example = "USD")
        String currency,

        @Schema(description = "국가 코드", example = "US")
        String country,

        @Schema(description = "거래소 MIC", example = "XNAS")
        String mic,

        @Schema(description = "세션 캘린더 키")
        String sessionCalendar,

        @Schema(description = "활성 여부", example = "true")
        Boolean isActive,

        @Schema(description = "추가 메타데이터 (JSON)", example = "{}")
        String metadata,

        @Schema(description = "생성 시각")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createDatetime,

        @Schema(description = "수정 시각")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime modifyDatetime
) {
}

