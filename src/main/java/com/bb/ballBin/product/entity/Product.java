package com.bb.ballBin.product.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "product_id", length = 36)
    private String productId;

    @Column(nullable = false, name = "product_name", length = 255)
    private String productName;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Entity to DTO 변환
     */
    public ProductResponseDto toDto() {
        return ProductResponseDto.builder()
                .productId(this.productId)
                .productName(this.productName)
                .productDescription(this.productDescription)
                .price(this.price)
                .stockQuantity(this.stockQuantity)
                .category(this.category)
                .imageUrl(this.imageUrl)
                .build();
    }

    /**
     * DTO 값으로 업데이트
     */
    public void updateFromDto(ProductRequestDto dto) {
        this.productName = dto.getProductName();
        this.productDescription = dto.getProductDescription();
        this.price = dto.getPrice();
        this.stockQuantity = dto.getStockQuantity();
        this.category = dto.getCategory();
        this.imageUrl = dto.getImageUrl();
    }
}
