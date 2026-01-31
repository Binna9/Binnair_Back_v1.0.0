package com.bin.web.bookmark.model;

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

    @Schema(description = "타켓 ID")
    private String targetId;
}
