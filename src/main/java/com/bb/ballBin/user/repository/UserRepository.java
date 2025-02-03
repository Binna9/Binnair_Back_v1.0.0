package com.bb.ballBin.user.repository;

import com.bb.ballBin.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByUserId(String userId);
}
