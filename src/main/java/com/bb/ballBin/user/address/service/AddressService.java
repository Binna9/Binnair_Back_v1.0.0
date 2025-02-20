package com.bb.ballBin.user.address.service;

import com.bb.ballBin.user.address.entity.Address;
import com.bb.ballBin.user.address.model.AddressRequestDto;
import com.bb.ballBin.user.address.model.AddressResponseDto;
import com.bb.ballBin.user.address.repository.AddressRepository;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * 특정 사용자의 배송지 목록 조회
     */
    public List<AddressResponseDto> getUserAddresses(String userId) {
        return addressRepository.findByUserUserId(userId).stream()
                .map(Address::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 배송지 추가
     */
    public AddressResponseDto addAddress(String userId, AddressRequestDto addressRequestDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("error.user.notfound"));

        // 기존 기본 배송지를 비활성화
        if (Boolean.TRUE.equals(addressRequestDto.getIsDefault())) {
            List<Address> addresses = addressRepository.findByUserUserId(userId);
            addresses.forEach(addr -> addr.setIsDefault(false));
            addressRepository.saveAll(addresses);  // 변경된 값 저장
        }

        Address address = Address.builder()
                .user(user)
                .receiver(addressRequestDto.getReceiver())
                .phone(addressRequestDto.getPhone())
                .postalCode(addressRequestDto.getPostalCode())
                .address1(addressRequestDto.getAddress1())
                .address2(addressRequestDto.getAddress2())
                .isDefault(Boolean.TRUE.equals(addressRequestDto.getIsDefault())) // null 방지
                .build();

        addressRepository.save(address);
        return address.toDto();
    }

    /**
     * 배송지 수정
     */
    public AddressResponseDto updateAddress(String addressId, AddressRequestDto addressRequestDto) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("error.address.notfound"));

        // 기본 배송지 변경 로직 (다른 배송지 기본 설정 해제)
        if (addressRequestDto.getIsDefault()) {
            addressRepository.findByUserUserId(address.getUser().getUserId()).forEach(addr -> addr.setIsDefault(false));
        }

        address.setReceiver(addressRequestDto.getReceiver());
        address.setPhone(addressRequestDto.getPhone());
        address.setPostalCode(addressRequestDto.getPostalCode());
        address.setAddress1(addressRequestDto.getAddress1());
        address.setAddress2(addressRequestDto.getAddress2());
        address.setIsDefault(addressRequestDto.getIsDefault());

        addressRepository.save(address);
        return address.toDto();
    }

    /**
     * 배송지 삭제
     */
    public void removeAddress(String addressId) {
        addressRepository.deleteById(addressId);
    }
}
