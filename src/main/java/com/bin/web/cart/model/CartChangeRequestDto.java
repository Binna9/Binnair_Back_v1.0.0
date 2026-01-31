package com.bin.web.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartChangeRequestDto {

    @Schema(description = "카트 ID")
    private String cartId;
    @Schema(description = "제품 ID")
    private String productId;
}
