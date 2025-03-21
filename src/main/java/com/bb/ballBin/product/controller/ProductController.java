package com.bb.ballBin.product.controller;

import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import com.bb.ballBin.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
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

    @GetMapping("")
    @Operation(summary = "모든 제품 조회")
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @PageableDefault(page = 0, size = 9, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productService.allProducts(pageable));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "개별 제품 조회")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable String productId) {
        return ResponseEntity.ok(productService.productById(productId));
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
    @MessageKey(value = "success.product.create")
    @Operation(summary = "제품 등록",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = ProductResponseDto.class)
                    )
            ))
    public ResponseEntity<Void> createProduct(@ModelAttribute ProductRequestDto productRequestDto,
                                              @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        productService.addProduct(productRequestDto, files);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{productId}")
    @Operation(summary = "제품 수정")
    @MessageKey(value = "success.product.update")
    public ResponseEntity<Void> modifyProduct(@PathVariable String productId, @RequestBody ProductRequestDto productRequestDto) {

        productService.updateProduct(productId, productRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "제품 삭제")
    @MessageKey(value = "success.product.delete")
    public ResponseEntity<Void> removeProduct(@PathVariable String productId) {

        productService.deleteProduct(productId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
