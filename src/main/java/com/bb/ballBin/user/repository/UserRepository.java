package com.bb.ballBin.user.repository;

import com.bb.ballBin.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

    Boolean existsByUserName(String username);
    User findByUserName(String username);
}
