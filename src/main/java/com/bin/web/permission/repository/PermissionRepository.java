package com.bin.web.permission.repository;

import com.bin.web.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByPermissionName(String permission);
}
