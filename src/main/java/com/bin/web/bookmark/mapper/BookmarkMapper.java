package com.bin.web.bookmark.mapper;

import com.bin.web.bookmark.entity.Bookmark;
import com.bin.web.bookmark.model.BookmarkRequestDto;
import com.bin.web.bookmark.model.BookmarkResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    Bookmark toEntity(BookmarkRequestDto dto);

    BookmarkResponseDto toDto(Bookmark bookmark);
}
