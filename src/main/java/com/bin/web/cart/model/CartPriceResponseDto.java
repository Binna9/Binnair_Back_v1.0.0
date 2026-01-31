package com.bin.web.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartPriceResponseDto {

    @Schema(description = "총 금액")
    private String totalPrice;
    @Schema(description = "할인 금액")
    private String discountAmount;
    @Schema(description = "최종 금액")
    private String discountPrice;
}
