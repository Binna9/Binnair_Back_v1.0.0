//package com.bb.ballBin.cart.controller;
//
//import model.cart.com.bin.web.CartChangeRequestDto;
//import model.cart.com.bin.web.CartPriceResponseDto;
//import model.cart.com.bin.web.CartRequestDto;
//import model.cart.com.bin.web.CartResponseDto;
//import service.cart.com.bin.web.CartService;
//import com.bb.ballBin.common.message.annotation.MessageKey;
//import util.common.com.bin.web.SecurityUtil;
//import io.swagger.v3.oas.annotations.Operation;
//import lombok.EqualsAndHashCode;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@EqualsAndHashCode
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/carts")
//public class CartController {
//
//    private final CartService cartService;
//
//    @GetMapping("")
//    @Operation(summary = "사용자 장바구니 조회")
//    public ResponseEntity<Page<CartResponseDto>> getAllCarts(
//            @PageableDefault(page = 0, size = 9, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {
//        String userId = SecurityUtil.getCurrentUserId();
//        return ResponseEntity.ok(cartService.allCarts(userId, pageable));
//    }
//
//    @GetMapping("/{cartId}")
//    @Operation(summary = "개별 장바구니 조회")
//    public ResponseEntity<CartResponseDto> getCartById(@PathVariable String cartId) {
//        return ResponseEntity.ok(cartService.cartById(cartId));
//    }
//
//    @PostMapping("")
//    @Operation(summary = "장바구니 추가")
//    @MessageKey(value = "success.cart.create")
//    public ResponseEntity<CartResponseDto> createCart(@RequestBody CartRequestDto cartRequestDto) {
//
//        String userId = SecurityUtil.getCurrentUserId();
//
//        cartService.addCart(userId, cartRequestDto);
//
//        return ResponseEntity.ok().build();
//    }
//
//    @PutMapping("/{cartId}/quantity")
//    @Operation(summary = "장바구니 수량 변경")
//    @MessageKey(value = "success.cart.quantity")
//    public ResponseEntity<Void> updateCartQuantity( @PathVariable("cartId") String cartId, @RequestParam int quantity) {
//
//        cartService.updateCartQuantity(cartId, quantity);
//
//        return ResponseEntity.ok().build();
//    }
//
//    @PutMapping("/change")
//    @Operation(summary = "장바구니 제품 옵션 변경")
//    @MessageKey(value = "success.cart.option")
//    public ResponseEntity<Void> changeProduct(@RequestBody CartChangeRequestDto cartChangeRequestDto) {
//
//        cartService.changeProduct(cartChangeRequestDto);
//
//        return ResponseEntity.ok().build();
//    }
//
//    @PostMapping("/calculate")
//    @Operation(summary = "선택된 장바구니 아이템 할인 금액 계산")
//    public ResponseEntity<CartPriceResponseDto> calculateDiscountedTotal(@RequestBody List<String> cartIds) {
//        CartPriceResponseDto result = cartService.calculateDiscountedTotal(cartIds);
//        return ResponseEntity.ok(result);
//    }
//
//    @DeleteMapping("/{cartId}")
//    @Operation(summary = "장바구니 삭제")
//    @MessageKey(value = "success.cart.delete")
//    public ResponseEntity<Void> deleteCart(@PathVariable String cartId) {
//
//        cartService.removeCart(cartId);
//
//        return ResponseEntity.ok().build();
//    }
//}
