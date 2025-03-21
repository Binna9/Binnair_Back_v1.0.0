package com.bb.ballBin.board.mapper;

import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BoardMapper {

    Board toEntity(BoardRequestDto dto);

    @Mapping(target = "writerId", source = "writer.userId")
    @Mapping(target = "comments", ignore = true)
    BoardResponseDto toDto(Board entity);

    void updateEntity(BoardRequestDto dto, @MappingTarget Board entity);
}
