package com.bb.ballBin.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartRequestDto {

    @Schema(description = "장바구니에 추가할 제품 ID")
    private String productId;

    @Schema(description = "수량")
    private Integer quantity;
}
