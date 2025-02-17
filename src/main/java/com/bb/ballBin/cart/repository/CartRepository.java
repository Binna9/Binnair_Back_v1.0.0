package com.bb.ballBin.cart.repository;

import com.bb.ballBin.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, String> {
    List<Cart> findByUserUserId(String userId);
}
