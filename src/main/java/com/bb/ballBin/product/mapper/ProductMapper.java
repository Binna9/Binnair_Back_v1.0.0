package com.bb.ballBin.product.mapper;

import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(ProductRequestDto dto);

    ProductResponseDto toDto(Product entity);

    void updateEntity(ProductRequestDto dto, @MappingTarget Product entity);
}
