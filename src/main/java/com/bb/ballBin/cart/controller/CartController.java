package com.bb.ballBin.cart.controller;

import com.bb.ballBin.cart.model.CartRequestDto;
import com.bb.ballBin.cart.model.CartResponseDto;
import com.bb.ballBin.cart.model.QuantityRequestDto;
import com.bb.ballBin.cart.service.CartService;
import com.bb.ballBin.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    /**
     * 현재 로그인한 사용자의 장바구니 조회
     */
    @GetMapping("")
    @Operation(summary = "현재 로그인한 사용자의 장바구니 조회")
    public ResponseEntity<List<CartResponseDto>> getUserCarts() {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(cartService.getUserCarts(userId));
    }

    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> getCartTotal() {

        String userId = SecurityUtil.getCurrentUserId(); // ✅ 현재 로그인한 사용자 ID 가져오기
        BigDecimal totalAmount = cartService.getTotalAmountByUser(userId);

        // ✅ Null 값 방지: 장바구니가 비어 있으면 0으로 반환
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("totalAmount", totalAmount);

        return ResponseEntity.ok(response);
    }


    /**
     * 장바구니 추가 (현재 로그인한 사용자)
     */
    @PostMapping("")
    @Operation(summary = "장바구니 추가")
    public ResponseEntity<CartResponseDto> addCart(@RequestBody CartRequestDto cartRequestDto) {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addCart(userId, cartRequestDto));
    }

    /**
     * 장바구니 수량 수정
     */
    @PutMapping("/{cartId}")
    @Operation(summary = "장바구니 수량 수정")
    public ResponseEntity<String> updateCart(@PathVariable("cartId") String cartId, @RequestBody QuantityRequestDto quantityRequestDto) {
        String userId = SecurityUtil.getCurrentUserId();

        cartService.updateCart(cartId, userId, quantityRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();  // userId 전달
    }


    /**
     * 장바구니 삭제
     */
    @DeleteMapping("/{cartId}")
    @Operation(summary = "장바구니 삭제")
    public ResponseEntity<Void> removeCart(@PathVariable String cartId) {
        cartService.removeCart(cartId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
