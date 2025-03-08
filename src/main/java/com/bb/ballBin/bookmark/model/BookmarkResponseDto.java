package com.bb.ballBin.bookmark.model;

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
public class BookmarkResponseDto {

    @Schema(description = "즐겨찾기 ID")
    private String bookmarkId;

    @Schema(description = "사용자 ID")
    private String userId;

    @Schema(description = "제품 ID")
    private String productId;

    @Schema(description = "제품명")
    private String productName;

    @Schema(description = "제품 설명")
    private String productDescription;

    @Schema(description = "제품 가격")
    private BigDecimal price;
}
