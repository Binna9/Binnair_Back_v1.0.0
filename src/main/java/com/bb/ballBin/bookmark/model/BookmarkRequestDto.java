package com.bb.ballBin.bookmark.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class BookmarkRequestDto {

    @Schema(description = "즐겨찾기할 제품 ID")
    private String productId;
}
