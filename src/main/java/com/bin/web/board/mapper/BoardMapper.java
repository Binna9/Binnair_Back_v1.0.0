package com.bin.web.board.mapper;

import com.bin.web.board.entity.Board;
import com.bin.web.board.model.BoardRequestDto;
import com.bin.web.board.model.BoardResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BoardMapper {

    Board toEntity(BoardRequestDto dto);

    @Mapping(target = "writerId", source = "writer.userId")
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "files", ignore = true)
    BoardResponseDto toDto(Board entity);

    void updateEntity(BoardRequestDto dto, @MappingTarget Board entity);
}
