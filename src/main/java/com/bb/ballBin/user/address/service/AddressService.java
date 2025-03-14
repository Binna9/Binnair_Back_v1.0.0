package com.bb.ballBin.user.address.service;

import com.bb.ballBin.common.exception.NotFoundException;
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
    @Transactional
    public AddressResponseDto addAddress(String userId, AddressRequestDto addressRequestDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        boolean hasDefault = addressRepository.existsByUser_UserIdAndDefaultAddressIndexIsNotNull(userId);

        Address address = Address.builder()
                .user(user)
                .receiver(addressRequestDto.getReceiver())
                .phone(addressRequestDto.getPhone())
                .postalCode1(addressRequestDto.getPostalCode1())
                .postalCode2(addressRequestDto.getPostalCode2())
                .postalCode3(addressRequestDto.getPostalCode3())
                .address1(addressRequestDto.getAddress1())
                .address2(addressRequestDto.getAddress2())
                .address3(addressRequestDto.getAddress3())

                .defaultAddressIndex(hasDefault ? null : 1) // 첫 번째 주소는 기본 배송지로 설정
                .build();

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
