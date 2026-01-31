package com.bin.web.address.repository;

import com.bin.web.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, String> {

    List<Address> findByUserUserId(String userId);

    @Query("SELECT a FROM Address a WHERE a.user.userId = :userId AND a.isDefault = true")
    Optional<Address> findDefaultAddressByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(a) > 0 FROM Address a WHERE a.user.userId = :userId")
    boolean existsByUserId(@Param("userId") String userId);

    boolean existsByUserUserIdAndAddress(String userId, String address);

    Optional<Address> findFirstByUserUserIdOrderByCreateDatetimeDesc(String userId);
}
