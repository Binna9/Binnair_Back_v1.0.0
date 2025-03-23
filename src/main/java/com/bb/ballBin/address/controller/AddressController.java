package com.bb.ballBin.address.controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.address.model.AddressRequestDto;
import com.bb.ballBin.address.model.AddressResponseDto;
import com.bb.ballBin.address.service.AddressService;
import com.bb.ballBin.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("")
    @Operation(summary = "현재 로그인한 사용자의 배송지 조회")
    public ResponseEntity<List<AddressResponseDto>> getUserAddresses() {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @PostMapping("")
    @Operation(summary = "배송지 추가")
    @MessageKey(value = "success.address.create")
    public ResponseEntity<Void> createAddress(@RequestBody AddressRequestDto addressRequestDto) {

        String userId = SecurityUtil.getCurrentUserId();
        addressService.addAddress(userId, addressRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{addressId}/default")
    @Operation(summary = "기본 배송지 변경")
    public ResponseEntity<Void> updateDefaultAddress(@PathVariable String addressId) {

        String userId = SecurityUtil.getCurrentUserId();
        addressService.defaultAddress(userId, addressId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "배송지 삭제")
    @MessageKey(value = "success.address.delete")
    public ResponseEntity<Void> removeAddress(@PathVariable String addressId) {
        addressService.removeAddress(addressId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
