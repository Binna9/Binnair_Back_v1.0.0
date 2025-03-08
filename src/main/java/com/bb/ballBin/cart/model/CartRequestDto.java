package com.bb.ballBin.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class CartRequestDto {

    @Schema(description = "장바구니에 추가할 제품 ID")
    private String productId;

    @Schema(description = "수량")
    private Integer quantity;
}
