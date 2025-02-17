package com.bb.ballBin.cart.controller;

import com.bb.ballBin.cart.model.CartRequestDto;
import com.bb.ballBin.cart.model.CartResponseDto;
import com.bb.ballBin.cart.service.CartService;
import com.bb.ballBin.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<CartResponseDto> updateCart(@PathVariable String cartId, @RequestBody CartRequestDto cartRequestDto) {
        return ResponseEntity.ok(cartService.updateCart(cartId, cartRequestDto));
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
