package com.bb.ballBin.product.controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import com.bb.ballBin.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("")
    @Operation(summary = "모든 제품 조회")
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @PageableDefault(page = 0, size = 9, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "개별 제품 조회")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable String productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/list")
    @Operation(summary = "제품별 카테고리 리스트 조회")
    public ResponseEntity<List<String>> getDistinctCategoryList() {
        List<String> categories = productService.getDistinctCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/image")
    @Operation(summary = "제품 이미지 반환")
    public ResponseEntity<Resource> getProductImage() {

        String productId = SecurityUtil.getCurrentUserId();

        return productService.getProductImage(productId);
    }

    @PostMapping("")
    @Operation(summary = "제품 등록")
    @MessageKey(value = "success.create")
    public ResponseEntity<String> createProduct(@ModelAttribute ProductRequestDto productRequestDto) {

        productService.createProduct(productRequestDto);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{productId}")
    @Operation(summary = "제품 수정")
    @MessageKey(value = "success.update")
    public ResponseEntity<String> updateProduct(
            @PathVariable String productId,
            @RequestBody ProductRequestDto productRequestDto) {

        productService.updateProduct(productId, productRequestDto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "제품 삭제")
    @MessageKey(value = "success.delete")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {

        productService.deleteProduct(productId);

        return ResponseEntity.ok().build();
    }
}
