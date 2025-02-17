package com.bb.ballBin.product.repository;

import com.bb.ballBin.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
