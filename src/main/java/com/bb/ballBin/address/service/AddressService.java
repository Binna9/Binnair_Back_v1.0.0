//package com.bb.ballBin.address.service;
//
//import com.bb.ballBin.address.entity.Address;
//import com.bb.ballBin.address.mapper.AddressMapper;
//import com.bb.ballBin.address.model.AddressRequestDto;
//import com.bb.ballBin.address.model.AddressResponseDto;
//import com.bb.ballBin.address.repository.AddressRepository;
//import com.bb.ballBin.common.exception.NotFoundException;
//import com.bb.ballBin.user.entity.User;
//import com.bb.ballBin.user.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class AddressService {
//
//    private final AddressRepository addressRepository;
//    private final UserRepository userRepository;
//    private final AddressMapper addressMapper;
//
//    /**
//     * 특정 사용자의 배송지 목록 조회
//     */
//    public List<AddressResponseDto> getUserAddresses(String userId) {
//        return addressRepository.findByUserUserId(userId).stream()
//                .map(addressMapper::toDto)
//                .collect(Collectors.toList());
//    }
//
//
//    /**
//     * 배송지 추가
//     */
//    public void addAddress(String userId, AddressRequestDto addressRequestDto) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException("error.user.notfound"));
//
//        if (addressRepository.existsByUserUserIdAndAddress(userId, addressRequestDto.getAddress())) {
//            throw new RuntimeException("error.address.duplicate");
//        }
//
//        boolean hasExistingAddresses = addressRepository.existsByUserId(userId);
//
//        Address address = addressMapper.toEntity(addressRequestDto);
//        address.setUser(user);
//        address.setDefault(!hasExistingAddresses);
//
//        addressRepository.save(address);
//    }
//
//    public void defaultAddress(String userId, String addressId) {
//
//        userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException("error.user.notfound"));
//
//        Address newDefaultAddress = addressRepository.findById(addressId)
//                .orElseThrow(() -> new NotFoundException("해당 배송지가 존재하지 않습니다."));
//
//        addressRepository.findDefaultAddressByUserId(userId)
//                .ifPresent(existingDefault -> {
//                    existingDefault.setDefault(false);
//                    addressRepository.save(existingDefault);
//                });
//
//        newDefaultAddress.setDefault(true);
//
//        addressRepository.save(newDefaultAddress);
//    }
//
//    /**
//     * 배송지 삭제
//     */
//    public void removeAddress(String addressId) {
//        Address address = addressRepository.findById(addressId)
//                .orElseThrow(() -> new NotFoundException("해당 주소가 존재하지 않습니다."));
//
//        addressRepository.deleteById(addressId);
//
//        if (address.isDefault()) {
//            addressRepository.findFirstByUserUserIdOrderByCreateDatetimeDesc(address.getUser().getUserId())
//                    .ifPresent(nextDefaultAddress -> {
//                        nextDefaultAddress.setDefault(true);
//                        addressRepository.save(nextDefaultAddress);
//                    });
//        }
//    }
//}
