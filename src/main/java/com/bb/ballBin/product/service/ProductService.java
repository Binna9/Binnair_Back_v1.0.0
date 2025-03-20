package com.bb.ballBin.product.service;

import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.file.entity.File;
import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.file.repository.FileRepository;
import com.bb.ballBin.file.service.FileService;
import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.product.mapper.ProductMapper;
import com.bb.ballBin.product.model.ProductRequestDto;
import com.bb.ballBin.product.model.ProductResponseDto;
import com.bb.ballBin.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FileService fileService;
    private final FileUtil fileUtil;
    private final FileRepository fileRepository;

    /**
     * 모든 제품 조회
     */
    public Page<ProductResponseDto> allProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toDto);
    }

    /**
     * 개별 제품 조회
     */
    public ProductResponseDto productById(String productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("error.product.notfound"));

        return productMapper.toDto(product);
    }

    /**
     * 제품 카테고리 조회
     */
    public List<String> getDistinctCategories() {
        return productRepository.findDistinctCategories();
    }

    /**
     * 제품 이미지 반환
     */
    public ResponseEntity<Resource> getProductImage(String productId) {

        List<File> files = fileRepository.findByTargetIdAndTargetType(productId, TargetType.PRODUCT);

        if (files == null || files.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        File fileEntity = files.get(0);
        String relativePath = fileEntity.getFilePath();

        if (relativePath == null || relativePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return fileUtil.getImageResponse("product", relativePath);
    }

    /**
     * 제품 등록
     */
    @Transactional
    public void addProduct(ProductRequestDto productRequestDto) {
        try {
            Product product = productMapper.toEntity(productRequestDto);

            productRepository.save(product);

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 제품 수정
     */
    public void updateProduct(String productId, ProductRequestDto productRequestDto) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("error.product.notfound"));

            productMapper.updateEntity(productRequestDto, product);

            productRepository.save(product);

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 제품 삭제
     */
    public void deleteProduct(String productId) {
        try {
            productRepository.deleteById(productId);
            fileService.deleteFilesByTarget(TargetType.PRODUCT, productId);
        } catch (Exception e) {
            logger.error("삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("삭제 중 오류 발생", e);
        }
    }
}
