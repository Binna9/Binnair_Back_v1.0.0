package com.bb.ballBin.user.address.repository;

import com.bb.ballBin.user.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, String> {
    List<Address> findByUserUserId(String userId);
}
