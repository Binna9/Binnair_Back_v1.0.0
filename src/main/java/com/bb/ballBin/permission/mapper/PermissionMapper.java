package com.bb.ballBin.permission.mapper;

import com.bb.ballBin.permission.entity.Permission;
import com.bb.ballBin.permission.model.PermissionRequestDto;
import com.bb.ballBin.permission.model.PermissionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    Permission toEntity(PermissionRequestDto dto);

    PermissionResponseDto toDto(Permission entity);

    void updateEntity(PermissionRequestDto dto, @MappingTarget Permission entity);
}
