package com.bin.web.permission.mapper;

import com.bin.web.permission.entity.Permission;
import com.bin.web.permission.model.PermissionRequestDto;
import com.bin.web.permission.model.PermissionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    Permission toEntity(PermissionRequestDto dto);

    PermissionResponseDto toDto(Permission entity);

    void updateEntity(PermissionRequestDto dto, @MappingTarget Permission entity);
}
