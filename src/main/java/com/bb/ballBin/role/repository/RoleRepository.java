package com.bb.ballBin.role.repository;

import com.bb.ballBin.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, String> {
    @Query("SELECT r FROM Role r WHERE r.roleName IN :roleNames")
    Set<Role> findByRoleNameIn(@Param("roleNames") List<String> roleNames);
}
