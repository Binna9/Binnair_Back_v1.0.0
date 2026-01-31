package com.bin.web.product.mapper;

import com.bin.web.product.entity.Product;
import com.bin.web.product.model.ProductRequestDto;
import com.bin.web.product.model.ProductResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(ProductRequestDto dto);

    ProductResponseDto toDto(Product entity);

    void updateEntity(ProductRequestDto dto, @MappingTarget Product entity);
}
