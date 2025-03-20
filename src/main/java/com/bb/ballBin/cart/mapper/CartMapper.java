package com.bb.ballBin.cart.mapper;

import com.bb.ballBin.cart.entity.Cart;
import com.bb.ballBin.cart.model.CartRequestDto;
import com.bb.ballBin.cart.model.CartResponseDto;
import com.bb.ballBin.user.model.UserUpdateRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CartMapper {

    Cart toEntity(CartRequestDto dto);

    CartResponseDto toDto(Cart entity);

    void updateEntity(UserUpdateRequestDto dto, @MappingTarget Cart entity);
}