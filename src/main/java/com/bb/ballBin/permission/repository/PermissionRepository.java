package com.bb.ballBin.permission.repository;

import com.bb.ballBin.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByPermissionName(String permission);
}
