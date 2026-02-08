package com.bin.anomaly.filter.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 거래소 정보 응답 모델
 */
@Schema(description = "거래소 정보")
public record VenueResponse(
        @Schema(description = "거래소 ID", example = "1")
        Long venueId,

        @Schema(description = "거래소 코드", example = "binance")
        String venueCode,

        @Schema(description = "거래소 타입", example = "exchange")
        String venueType,

        @Schema(description = "타임존", example = "UTC")
        String timezone,

        @Schema(description = "활성 여부", example = "true")
        Boolean isActive,

        @Schema(description = "추가 메타데이터 (JSON)", example = "{}")
        String metadata,

        @Schema(description = "생성 시각")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createDatetime
) {
}

