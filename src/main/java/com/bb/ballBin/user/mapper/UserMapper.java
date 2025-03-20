package com.bb.ballBin.user.mapper;

import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.model.UserUpdateRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequestDto dto);

    UserResponseDto toDto(User entity);

    void updateEntity(UserUpdateRequestDto dto, @MappingTarget User entity);
}
