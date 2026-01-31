package com.bin.web.user.repository;

import com.bin.web.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    @Query(value = """
        UPDATE web.users
        SET failed_login_attempts = COALESCE(failed_login_attempts, 0) + 1,
            modify_datetime = now()
        WHERE login_id = :loginId
        RETURNING failed_login_attempts
        """, nativeQuery = true)
    Integer incrementFailedAttempts(@Param("loginId") String loginId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)",
            nativeQuery = true)
    void insertUserRole(@Param("userId") String userId, @Param("roleId") String roleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId AND role_id = :roleId",
            nativeQuery = true)
    void deleteUserRole(@Param("userId") String userId, @Param("roleId") String roleId);

    @Transactional
    @Query(value = "SELECT COUNT(*) > 0 FROM user_roles WHERE user_id = :userId AND role_id = :roleId",
            nativeQuery = true)
    boolean existsUserRole(@Param("userId") String userId, @Param("roleId") String roleId);
}
