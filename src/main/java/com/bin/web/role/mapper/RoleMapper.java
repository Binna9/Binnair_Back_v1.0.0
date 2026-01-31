package com.bin.web.role.mapper;

import com.bin.web.role.entity.Role;
import com.bin.web.role.model.RoleRequestDto;
import com.bin.web.role.model.RoleResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    Role toEntity(RoleRequestDto dto);

    RoleResponseDto toDto(Role entity);

    void updateEntity(RoleRequestDto dto, @MappingTarget Role entity);
}
