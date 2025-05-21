package com.bb.ballBin.user.repository;

import com.bb.ballBin.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByUserId(String userId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByNickName(String nickName);
}
