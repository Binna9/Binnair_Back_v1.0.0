package com.bb.ballBin.cart.model;

import com.bb.ballBin.cart.entity.Cart;
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

    // ✅ Cart 엔티티를 기반으로 변환하는 생성자 추가
    public CartResponseDto(Cart cart) {
        this.cartId = cart.getCartId().toString();
        this.userId = cart.getUser().getUserId().toString();
        this.productId = cart.getProduct().getProductId().toString();
        this.productName = cart.getProduct().getProductName().toString();
        this.productDescription = cart.getProduct().getProductDescription().toString();
        this.quantity = cart.getQuantity();
        this.price = cart.getProduct().getPrice();
    }
}
