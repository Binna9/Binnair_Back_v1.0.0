package com.bb.ballBin.product.controller;

import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import com.bb.ballBin.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 모든 제품 조회
     */
    @GetMapping("")
    @Operation(summary = "모든 제품 조회")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * 개별 제품 조회
     */
    @GetMapping("/{productId}")
    @Operation(summary = "개별 제품 조회")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable String productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    /**
     * 제품 등록
     */
    @PostMapping("")
    @Operation(summary = "제품 등록")
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductRequestDto productRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(productRequestDto));
    }

    /**
     * 제품 수정
     */
    @PutMapping("/{productId}")
    @Operation(summary = "제품 수정")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable String productId, @RequestBody ProductRequestDto productRequestDto) {
        return ResponseEntity.ok(productService.updateProduct(productId, productRequestDto));
    }

    /**
     * 제품 삭제
     */
    @DeleteMapping("/{productId}")
    @Operation(summary = "제품 삭제")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
