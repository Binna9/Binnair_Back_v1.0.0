package com.bb.ballBin.product.controller;

import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import com.bb.ballBin.product.repository.ProductRepository;
import com.bb.ballBin.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final FileUtil fileUtil;

    /**
     * 모든 제품 조회
     */
    @GetMapping("")
    @Operation(summary = "모든 제품 조회 (페이징 처리)")
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @PageableDefault(page = 0, size = 10, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
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
     * 제품 등록 (이미지 포함 가능)
     */
    @PostMapping("")
    @Operation(summary = "제품 등록")
    public ResponseEntity<ProductResponseDto> createProduct(
            @ModelAttribute ProductRequestDto productRequestDto,
            @RequestPart(value = "productFile", required = false) MultipartFile file) {

        ProductResponseDto responseDto = productService.createProduct(productRequestDto, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
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

    /**
     * 제품 이미지 반환
     */
    @GetMapping("/{productId}/image")
    @Operation(summary = "제품 이미지 반환")
    public ResponseEntity<Resource> getProductImage(@PathVariable String productId) {
        return productRepository.findById(productId)
                .map(product -> {
                    String relativePath = product.getImageUrl();
                    if (relativePath == null || relativePath.isEmpty()) {
                        System.out.println("❌ No image path found for product: " + productId);
                        return ResponseEntity.notFound().<Resource>build();
                    }

                    return fileUtil.getImageResponse("product", relativePath);
                })
                .orElseGet(() -> ResponseEntity.notFound().<Resource>build());
    }
}
