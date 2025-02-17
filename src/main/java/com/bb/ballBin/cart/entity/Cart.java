package com.bb.ballBin.cart.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.cart.model.CartResponseDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "carts")
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "cart_id", length = 36)
    private String cartId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, name = "quantity")
    private Integer quantity;

    /**
     * Entity to DTO 변환
     */
    public CartResponseDto toDto() {
        return CartResponseDto.builder()
                .cartId(this.cartId)
                .userId(this.user.getUserId())
                .productId(this.product.getProductId())
                .productName(this.product.getProductName())
                .quantity(this.quantity)
                .build();
    }
}
