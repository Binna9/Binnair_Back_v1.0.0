package com.bin.web.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartRequestDto {

    @Schema(description = "제품 ID")
    private String productId;

    @Schema(description = "수량")
    private Integer quantity;
}
