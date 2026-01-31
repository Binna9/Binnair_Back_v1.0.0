package com.bin.web.bookmark.model;

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

    @Schema(description = "타겟 ID")
    private String targetId;
}
