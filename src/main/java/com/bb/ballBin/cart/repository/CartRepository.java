package com.bb.ballBin.cart.repository;

import com.bb.ballBin.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    List<Cart> findByUserUserId(String userId);

    Optional<Cart> findByCartIdAndUser_UserId(String cartId, String userId);

    @Query("SELECT COALESCE(SUM(c.quantity * p.price), 0) " +
            "FROM Cart c JOIN c.product p " +
            "WHERE c.user.userId = :userId")
    BigDecimal calculateTotalAmountByUser(@Param("userId") String userId);
}
