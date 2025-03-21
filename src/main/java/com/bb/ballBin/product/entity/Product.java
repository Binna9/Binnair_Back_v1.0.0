package com.bb.ballBin.product.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "product_id")
    private String productId;

    @Column(nullable = false, name = "product_name")
    private String productName;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "category")
    private String category;

    @Builder.Default
    @Column(name = "discount_rate", nullable = false)
    private int discountRate = 0;

    @Column(name = "discount_amount", insertable = false, updatable = false)
    private BigDecimal discountAmount;

    @Column(name = "discount_price", insertable = false, updatable = false)
    private BigDecimal discountPrice;
}
