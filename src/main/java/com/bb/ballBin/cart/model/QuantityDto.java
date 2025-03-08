package com.bb.ballBin.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class QuantityDto {

    @Schema(description = "사용자 ID")
    private String userId;
    @Schema(description = "총 금액")
    private BigDecimal totalAmount;
    @Schema(description = "수량")
    private Integer quantity;
}
