package com.bb.ballBin.bookmark.mapper;

import com.bb.ballBin.bookmark.entity.Bookmark;
import com.bb.ballBin.bookmark.model.BookmarkRequestDto;
import com.bb.ballBin.bookmark.model.BookmarkResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    Bookmark toEntity(BookmarkRequestDto dto);

    BookmarkResponseDto toDto(Bookmark bookmark);
}
