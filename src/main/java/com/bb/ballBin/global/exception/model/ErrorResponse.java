package com.bb.ballBin.global.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Schema(description = "HTTP 상태 코드")
    private Integer statusCode;
    private String error;
    private String message;

}
