package com.bb.ballBin.common.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
        @Schema(description = "HTTP 상태 코드")
        int status,
        @Schema(description = "에러 메시지")
        String message
) {
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message);
    }
}
