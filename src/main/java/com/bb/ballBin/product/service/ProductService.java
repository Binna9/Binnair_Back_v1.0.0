package com.bb.ballBin.product.service;

import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import com.bb.ballBin.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final FileUtil fileUtil;

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
     * 제품 등록 (파일 포함)
     */
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto, MultipartFile file) {
        // 1️⃣ 먼저 제품을 저장
        Product newProduct = productRepository.save(productRequestDto.toEntity());

        // 2️⃣ 파일이 존재하면 저장 후 이미지 경로 업데이트
        if (file != null && !file.isEmpty()) {
            String filePath = fileUtil.saveFile("product", newProduct.getProductId(), file);
            newProduct.setImageUrl(filePath);
            productRepository.save(newProduct); // 이미지 경로 포함하여 다시 저장
        }

        // 3️⃣ DTO 변환 후 반환
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
