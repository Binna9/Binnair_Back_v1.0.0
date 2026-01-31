package com.bin.web.cart.mapper;

import com.bin.web.cart.entity.Cart;
import com.bin.web.cart.model.CartRequestDto;
import com.bin.web.cart.model.CartResponseDto;
import com.bin.web.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "product", source = "productId", qualifiedByName = "mapProduct")
    Cart toEntity(CartRequestDto dto);

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "productDescription", source = "product.productDescription")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "discountAmount", source = "product.discountAmount")
    @Mapping(target = "discountPrice", source = "product.discountPrice")
    @Mapping(target = "quantity", source = "quantity")
    CartResponseDto toDto(Cart cart);

    @Named("mapProduct")
    default Product mapProduct(String productId) {
        if (productId == null) {
            return null;
        }
        return Product.builder().productId(productId).build();
    }
}
