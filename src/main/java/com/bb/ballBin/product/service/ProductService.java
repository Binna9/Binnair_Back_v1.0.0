package com.bb.ballBin.product.service;

import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import com.bb.ballBin.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 모든 제품 조회
     */
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(Product::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 개별 제품 조회
     */
    public ProductResponseDto getProductById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("error.product.notfound"));
        return product.toDto();
    }

    /**
     * 제품 등록
     */
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        Product newProduct = productRepository.save(productRequestDto.toEntity());
        return newProduct.toDto();
    }

    /**
     * 제품 수정
     */
    public ProductResponseDto updateProduct(String productId, ProductRequestDto productRequestDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("error.product.notfound"));

        product.updateFromDto(productRequestDto);
        productRepository.save(product);
        return product.toDto();
    }

    /**
     * 제품 삭제
     */
    public void deleteProduct(String productId) {
        productRepository.deleteById(productId);
    }
}
