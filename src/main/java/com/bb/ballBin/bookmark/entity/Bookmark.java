package com.bb.ballBin.bookmark.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.bookmark.model.BookmarkResponseDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "bookmarks")
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "bookmark_id", length = 36)
    private String bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Entity to DTO 변환
     */
    public BookmarkResponseDto toDto() {
        return BookmarkResponseDto.builder()
                .bookmarkId(this.bookmarkId)
                .userId(this.user.getUserId())
                .productId(this.product.getProductId())
                .productName(this.product.getProductName())
                .productDescription(this.product.getProductDescription())
                .price(this.product.getPrice())
                .build();
    }
}
