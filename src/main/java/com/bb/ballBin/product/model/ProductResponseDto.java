package com.bb.ballBin.product.model;

import com.bb.ballBin.file.entity.File;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

    @Schema(description = "제품 할인율")
    private int discountRate;

    @Schema(description = "제품 할인 금액")
    private BigDecimal discountAmount;

    @Schema(description = "제품 할인 적용 가격")
    private BigDecimal discountPrice;

    @Schema(description = "제품 관련 파일")
    private List<File> files;
}
