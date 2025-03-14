package com.bb.ballBin.cart.controller;

import com.bb.ballBin.cart.model.CartRequestDto;
import com.bb.ballBin.cart.model.CartResponseDto;
import com.bb.ballBin.cart.service.CartService;
import com.bb.ballBin.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode
@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    @GetMapping("")
    @Operation(summary = "현재 로그인한 사용자의 장바구니 조회")
    public ResponseEntity<Map<String , Object>> getUserCarts() {

        String userId = SecurityUtil.getCurrentUserId();

        return ResponseEntity.ok(cartService.getUserCarts(userId));
    }

    @PostMapping("")
    @Operation(summary = "장바구니 추가")
    public ResponseEntity<CartResponseDto> addCart(@RequestBody CartRequestDto cartRequestDto) {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addCart(userId, cartRequestDto));
    }

    @PutMapping("/{cartId}/change-product")
    @Operation(summary = "장바구니 제품 옵션 변경")
    public ResponseEntity<CartResponseDto> changeProduct(
            @PathVariable("cartId") String cartId,
            @RequestParam("productId") String productId) {

        CartResponseDto updatedCart = cartService.changeProduct(cartId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    @PutMapping("/update-quantity")
    @Operation(summary = "장바구니 수량 변경")
    public ResponseEntity<Void> updateCartQuantity(@RequestParam String cartId, @RequestParam int quantity) {

        cartService.updateCartQuantity(cartId, quantity);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/discounted-total")
    public ResponseEntity<Map<String, BigDecimal>> calculateDiscountedTotal(
            @RequestBody Map<String, List<String>> request) { // ✅ JSON 객체로 받음

        List<String> cartIds = request.get("cartIds"); // JSON 에서 cartIds 추출

        Map<String, BigDecimal> result = cartService.calculateDiscountedTotal(cartIds);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{cartId}")
    @Operation(summary = "장바구니 삭제")
    public ResponseEntity<Void> removeCart(@PathVariable String cartId) {
        cartService.removeCart(cartId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
