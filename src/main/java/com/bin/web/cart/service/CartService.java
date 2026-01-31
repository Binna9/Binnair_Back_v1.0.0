package com.bin.web.cart.service;

import com.bin.web.cart.entity.Cart;
import com.bin.web.cart.mapper.CartMapper;
import com.bin.web.cart.model.CartChangeRequestDto;
import com.bin.web.cart.model.CartPriceResponseDto;
import com.bin.web.cart.model.CartRequestDto;
import com.bin.web.cart.model.CartResponseDto;
import com.bin.web.cart.repository.CartRepository;
import com.bin.web.common.exception.NotFoundException;
import com.bin.web.common.util.SecurityUtil;
import com.bin.web.product.entity.Product;
import com.bin.web.product.repository.ProductRepository;
import com.bin.web.user.entity.User;
import com.bin.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 사용자 장바구니 전체 조회
     */
    @Transactional
    public Page<CartResponseDto> allCarts(String userId, Pageable pageable) {
        return cartRepository.findByUserUserId(userId, pageable)
                .map(cartMapper::toDto);
    }

    /**
     * 사용자 장바구니 개별 조회
     */
    public CartResponseDto cartById(String cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("error.product.notfound"));

        return cartMapper.toDto(cart);
    }

    /**
     * 장바구니 추가
     */
    @Transactional
    public void addCart(String userId, CartRequestDto cartRequestDto) {
        try {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("error.user.notfound"));

            Cart cart = cartMapper.toEntity(cartRequestDto);
            cart.setUser(user);

            cartRepository.save(cart);

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오류 발생", e);
        }
    }

    /**
     * 장바구니 수량 변경
     */
    @Transactional
    public void updateCartQuantity(String cartId, int quantity) {
        try {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new RuntimeException("error.cart.notfound"));

            cart.setQuantity(quantity);
            cartRepository.save(cart);
        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오류 발생", e);
        }
    }

    /**
     * 장바구니 제품 옵션 변경
     */
    public void changeProduct(CartChangeRequestDto requestDto) {
        try {
            String userId = SecurityUtil.getCurrentUserId();

            Cart cart = cartRepository.findByCartIdAndUser_UserId(requestDto.getCartId(), userId)
                    .orElseThrow(() -> new RuntimeException("error.cart.notfound"));

            Product product = productRepository.findById(requestDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("선택한 옵션이 유효하지 않음"));

            cart.setProduct(product);

            cartRepository.save(cart);
        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오류 발생", e);
        }
    }

    /**
     * 장바구니 삭제
     */
    public void removeCart(String cartId) {
        try {
            cartRepository.deleteById(cartId);
        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오류 발생", e);
        }
    }

    /**
     * 할인율 적용된 총 금액과 할인된 금액 반환
     */
    public CartPriceResponseDto calculateDiscountedTotal(List<String> cartIds) {
        try {
            List<Object[]> result = cartRepository.calculateTotalAndDiscount(cartIds);

            if (result.isEmpty() || result.get(0)[0] == null) {
                throw new RuntimeException("error.cart.notfound");
            }

            BigDecimal totalAmount = (BigDecimal) result.get(0)[0]; // 할인 전 총 금액
            BigDecimal totalDiscountAmount = (BigDecimal) result.get(0)[1]; // 총 할인 금액

            BigDecimal discountedTotal = totalAmount.subtract(totalDiscountAmount); // 할인 후 총 금액

            return new CartPriceResponseDto(
                    totalAmount.toString(),
                    totalDiscountAmount.toString(),
                    discountedTotal.toString()
            );
        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오류 발생", e);
        }
    }
}
