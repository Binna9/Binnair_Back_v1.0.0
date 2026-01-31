package com.bin.web.product.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDto {

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

    @Schema(description = "제품 할인율")
    private int discountRate;
}
