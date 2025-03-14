package com.bb.ballBin.cart.service;

import com.bb.ballBin.cart.entity.Cart;
import com.bb.ballBin.cart.model.CartRequestDto;
import com.bb.ballBin.cart.model.CartResponseDto;
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
     * 사용자 장바구니 목록 및 금액 정보 반환
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserCarts(String userId) {

        List<Cart> cartList = cartRepository.findByUserUserId(userId);
        List<String> cartIds = cartList.stream().map(Cart::getCartId).collect(Collectors.toList());

        List<CartResponseDto> carts = cartList.stream()
                .map(Cart::toDto)
                .collect(Collectors.toList());

        Map<String, BigDecimal> discountData = calculateDiscountedTotal(cartIds);

        Map<String, Object> response = new HashMap<>();
        response.put("carts", carts); // 장바구니 목록
        response.put("totalAmount", discountData.get("totalAmount")); // 기존 총 금액 (할인 미적용)
        response.put("discountAmount", discountData.get("discountAmount")); // 총 할인 금액
        response.put("discountedTotal", discountData.get("discountedTotal")); // 할인 적용 후 금액

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
     * 장바구니 수량 변경 (할인 금액 반환 X)
     */
    @Transactional
    public void updateCartQuantity(String cartId, int quantity) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("error.cart.notfound"));

        cart.setQuantity(quantity);
        cartRepository.save(cart);
    }

//    /**
//     * 기존 총 금액 할인된 총 금액과 할인 금액 반환
//     */
//    @Transactional(readOnly = true)
//    public Map<String, BigDecimal> getDiscountedTotal(List<String> cartIds) {
//        List<Cart> carts = cartRepository.findAllById(cartIds);
//
//        if (carts.isEmpty()) {
//            throw new RuntimeException("error.cart.notfound");
//        }
//
//        // ✅ 기존 총 금액 계산 (할인 미적용)
//        BigDecimal totalAmount = carts.stream()
//                .map(cart -> cart.getProduct().getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // ✅ 전체 할인 금액 계산
//        BigDecimal totalDiscountAmount = carts.stream()
//                .map(cart -> {
//                    BigDecimal cartTotal = cart.getProduct().getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
//                    int discountRate = cart.getProduct().getDiscountRate();
//                    return cartTotal.multiply(BigDecimal.valueOf(discountRate)).divide(BigDecimal.valueOf(100));
//                })
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // ✅ 할인된 총 금액 계산
//        BigDecimal discountedTotal = totalAmount.subtract(totalDiscountAmount);
//
//        // ✅ 응답 데이터 구성
//        Map<String, BigDecimal> response = new HashMap<>();
//        response.put("totalAmount", totalAmount); // 기존 총 금액
//        response.put("discountAmount", totalDiscountAmount); // 총 할인 금액
//        response.put("discountedTotal", discountedTotal); // 할인 적용 후 금액
//
//        return response;
//    }

    /**
     * 장바구니 상품 옵션 변경
     */
    public CartResponseDto changeProduct(String cartId, String newProductId) {

        String userId = SecurityUtil.getCurrentUserId();

        Cart cart = cartRepository.findByCartIdAndUser_UserId(cartId, userId)
                .orElseThrow(() -> new RuntimeException("error.cart.notfound"));

        Product newProduct = productRepository.findById(newProductId)
                .orElseThrow(() -> new IllegalArgumentException("선택한 옵션이 유효하지 않음"));

        cart.setProduct(newProduct);
        cartRepository.save(cart);

        return new CartResponseDto(cart);
    }

    /**
     * 장바구니 삭제
     */
    public void removeCart(String cartId) {
        cartRepository.deleteById(cartId);
    }

    /**
     * 할인율 적용된 총 금액과 할인된 금액 반환
     */
    public Map<String, BigDecimal> calculateDiscountedTotal(List<String> cartIds) {

        List<Cart> carts = cartRepository.findAllById(cartIds);

        if (carts.isEmpty()) {
            throw new RuntimeException("error.cart.notfound");
        }

        BigDecimal totalAmount = BigDecimal.ZERO; // 총 금액 (할인 미적용)
        BigDecimal totalDiscountAmount = BigDecimal.ZERO; // 총 할인 금액

        for (Cart cart : carts) {
            BigDecimal cartTotal = cart.getProduct().getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())); // 개별 장바구니 아이템 총 금액
            int discountRate = cart.getProduct().getDiscountRate(); // 할인율
            BigDecimal discountAmount = cartTotal.multiply(BigDecimal.valueOf(discountRate)).divide(BigDecimal.valueOf(100)); // 개별 할인 금액

            totalAmount = totalAmount.add(cartTotal); // 할인 전 총 금액 누적
            totalDiscountAmount = totalDiscountAmount.add(discountAmount); // 총 할인 금액 누적
        }

        BigDecimal discountedTotal = totalAmount.subtract(totalDiscountAmount); // 할인 후 총 금액

        Map<String, BigDecimal> response = new HashMap<>();

        response.put("totalAmount", totalAmount); // 할인 적용 전 총 금액 추가
        response.put("discountedTotal", discountedTotal);
        response.put("discountAmount", totalDiscountAmount);

        return response;
    }
}
