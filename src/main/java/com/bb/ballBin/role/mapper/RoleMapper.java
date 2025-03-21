package com.bb.ballBin.role.mapper;

import com.bb.ballBin.role.entity.Role;
import com.bb.ballBin.role.model.RoleRequestDto;
import com.bb.ballBin.role.model.RoleResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    Role toEntity(RoleRequestDto dto);

    RoleResponseDto toDto(Role entity);

    void updateEntity(RoleRequestDto dto, @MappingTarget Role entity);
}
