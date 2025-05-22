package com.bb.ballBin.cart.repository;

import com.bb.ballBin.cart.entity.Cart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {

    Page<Cart> findByUserUserId(String userId, Pageable pageable);

    Optional<Cart> findByCartIdAndUser_UserId(String cartId, String userId);

    @Query("SELECT COALESCE(SUM(p.price * c.quantity), 0), COALESCE(SUM(p.discountPrice * c.quantity), 0), COALESCE(SUM(p.discountAmount * c.quantity), 0) " +
            "FROM Cart c JOIN c.product p " +
            "WHERE c.cartId IN :cartIds")
    List<Object[]> calculateTotalAndDiscount(@Param("cartIds") List<String> cartIds);

}

