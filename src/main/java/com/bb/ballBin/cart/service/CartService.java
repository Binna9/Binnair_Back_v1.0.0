package com.bb.ballBin.cart.service;

import com.bb.ballBin.cart.entity.Cart;
import com.bb.ballBin.cart.model.CartRequestDto;
import com.bb.ballBin.cart.model.CartResponseDto;
import com.bb.ballBin.cart.model.QuantityDto;
import com.bb.ballBin.cart.repository.CartRepository;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.product.entity.Product;
import com.bb.ballBin.product.repository.ProductRepository;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 특정 사용자의 장바구니 목록 조회
     */
    public Map<String, Object> getUserCarts(String userId) {
        List<CartResponseDto> carts = cartRepository.findByUserUserId(userId).stream()
                .map(Cart::toDto)
                .collect(Collectors.toList());

        BigDecimal totalAmount = cartRepository.calculateTotalAmountByUser(userId);
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("carts", carts);
        response.put("totalAmount", totalAmount);

        return response;
    }


    /**
     * 장바구니 추가
     */
    public CartResponseDto addCart(String userId, CartRequestDto cartRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("error.user.notfound"));

        Product product = productRepository.findById(cartRequestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("error.product.notfound"));

        Cart cart = Cart.builder()
                .user(user)
                .product(product)
                .quantity(cartRequestDto.getQuantity())
                .build();

        cartRepository.save(cart);
        return cart.toDto();
    }

    /**
     * 장바구니 수량 , 총 합 반환
     */
    @Transactional
    public QuantityDto updateCart(String cartId, QuantityDto quantityDto) {

        String userId = SecurityUtil.getCurrentUserId();

        Cart cart = cartRepository.findByCartIdAndUser_UserId(cartId, userId)
                .orElseThrow(() -> new RuntimeException("error.cart.notfound"));

        Integer newQuantity = quantityDto.getQuantity();

        if (newQuantity != null) {
            cart.setQuantity(newQuantity);
            cartRepository.save(cart);
        }

        BigDecimal totalAmount = cartRepository.calculateTotalAmountByUser(userId);

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        return QuantityDto.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .quantity(cart.getQuantity())
                .build();
    }

    /**
     * 장바구니 삭제
     */
    public void removeCart(String cartId) {
        cartRepository.deleteById(cartId);
    }
}
