package com.bb.ballBin.product.model;

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
public class ProductResponseDto {

    @Schema(description = "제품 ID")
    private String productId;

    @Schema(description = "제품명")
    private String productName;

    @Schema(description = "제품 설명")
    private String productDescription;

    @Schema(description = "가격")
    private BigDecimal price;

    @Schema(description = "재고 수량")
    private Integer stockQuantity;

    @Schema(description = "카테고리")
    private String category;

    @Schema(description = "이미지 URL")
    private String imageUrl;
}
