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

    /**
     * 현재 로그인한 사용자의 배송지 조회
     */
    @GetMapping("")
    @Operation(summary = "현재 로그인한 사용자의 배송지 조회")
    public ResponseEntity<List<AddressResponseDto>> getUserAddresses() {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    /**
     * 배송지 추가 (현재 로그인한 사용자)
     */
    @PostMapping("")
    @Operation(summary = "배송지 추가")
    @MessageKey(value = "success.address.create")
    public ResponseEntity<AddressResponseDto> addAddress(@RequestBody AddressRequestDto addressRequestDto) {

        String userId = SecurityUtil.getCurrentUserId();

        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.addAddress(userId, addressRequestDto));
    }



    /**
     * 배송지 삭제
     */
    @DeleteMapping("/{addressId}")
    @Operation(summary = "배송지 삭제")
    @MessageKey(value = "success.address.delete")
    public ResponseEntity<Void> removeAddress(@PathVariable String addressId) {
        addressService.removeAddress(addressId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
