package com.bb.ballBin.address.service;

import com.bb.ballBin.address.entity.Address;
import com.bb.ballBin.address.model.AddressRequestDto;
import com.bb.ballBin.address.model.AddressResponseDto;
import com.bb.ballBin.address.repository.AddressRepository;
import com.bb.ballBin.common.exception.NotFoundException;
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

        Address address = Address.builder()
                .user(user)
                .receiver(addressRequestDto.getReceiver())
                .phone(addressRequestDto.getPhone())
                .postalCode(addressRequestDto.getPostalCode())
                .address(addressRequestDto.getAddress())
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
