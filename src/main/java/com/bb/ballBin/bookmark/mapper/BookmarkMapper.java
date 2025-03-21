package com.bb.ballBin.bookmark.mapper;

import com.bb.ballBin.bookmark.entity.Bookmark;
import com.bb.ballBin.bookmark.model.BookmarkRequestDto;
import com.bb.ballBin.bookmark.model.BookmarkResponseDto;
import com.bb.ballBin.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    @Mapping(target = "product", source = "productId", qualifiedByName = "mapProduct")
    Bookmark toEntity(BookmarkRequestDto dto);

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "productDescription", source = "product.productDescription")
    @Mapping(target = "price", source = "product.price")
    BookmarkResponseDto toDto(Bookmark bookmark);

    @Named("mapProduct")
    default Product mapProduct(String productId) {
        if (productId == null) {
            return null;
        }
        return Product.builder().productId(productId).build();
    }
}
