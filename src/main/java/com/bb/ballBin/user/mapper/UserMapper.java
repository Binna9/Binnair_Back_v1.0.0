package com.bb.ballBin.user.mapper;

import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.model.UserUpdateRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequestDto dto);

    default UserResponseDto toDto(User entity) {
        if (entity == null) {
            return null;
        }

        UserResponseDto userResponseDto = new UserResponseDto();

        userResponseDto.setUserId(entity.getUserId());
        userResponseDto.setLoginId(entity.getLoginId());
        userResponseDto.setUserName(entity.getUserName());
        userResponseDto.setEmail(entity.getEmail());
        userResponseDto.setNickName(entity.getNickName());
        userResponseDto.setPhoneNumber(entity.getPhoneNumber());
        userResponseDto.setActive(entity.isActive());

        if (entity.getRoles() != null) {
            Set<String> roleNames = entity.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .collect(Collectors.toSet());
            userResponseDto.setRoles(roleNames);
        }

        return userResponseDto;
    }

    void updateEntity(UserUpdateRequestDto dto, @MappingTarget User entity);
}
