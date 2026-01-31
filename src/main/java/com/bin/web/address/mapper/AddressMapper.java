package com.bin.web.address.mapper;

import com.bin.web.address.entity.Address;
import com.bin.web.address.model.AddressRequestDto;
import com.bin.web.address.model.AddressResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address toEntity(AddressRequestDto dto);

    AddressResponseDto toDto(Address entity);

    void updateEntity(AddressRequestDto dto, @MappingTarget Address entity);
}
