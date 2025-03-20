package com.bb.ballBin.cart.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartResponseDto {

    @Schema(description = "장바구니 ID")
    private String cartId;
    @Schema(description = "사용자 ID")
    private String userId;
    @Schema(description = "제품 ID")
    private String productId;
    @Schema(description = "제품명")
    private String productName;
    @Schema(description = "제품 설명")
    private String productDescription;
    @Schema(description = "수량")
    private Integer quantity;
    @Schema(description = "제품 가격")
    private BigDecimal price;
    @Schema(description = "제품 할인 금액")
    private BigDecimal discountAmount;
    @Schema(description = "제품 최종 금액")
    private BigDecimal discountPrice;
}
