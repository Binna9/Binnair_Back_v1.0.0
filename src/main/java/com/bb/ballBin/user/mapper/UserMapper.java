package com.bb.ballBin.user.mapper;

import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.model.UserRequsetDto;
import com.bb.ballBin.user.model.UserResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequestDto dto);

    UserResponseDto toDto(User entity);

    void updateEntity(UserRequsetDto dto, @MappingTarget User entity);
}
