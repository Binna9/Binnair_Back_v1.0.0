package com.bb.ballBin.role.repository;

import com.bb.ballBin.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, String> {

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.roleId = :roleId")
    Optional<Role> findByRoleId(@Param("roleId") String roleId);

    @Query("SELECT DISTINCT r FROM Role r JOIN FETCH r.permissions WHERE r.roleId IN :roleIds")
    List<Role> findAllWithPermissionsByRoleIdIn(@Param("roleIds") List<String> roleIds);

    Optional<Role> findByRoleNameIgnoreCase(String roleName); // 대소문자 무시 (한글엔 영향 없음)

    @Query("SELECT r FROM Role r WHERE r.roleName IN :roleNames")
    Set<Role> findByRoleNameIn(@Param("roleNames") Collection<String> roleNames);

    @Query("SELECT r.roleId FROM Role r WHERE r.roleName IN :roleNames")
    Set<String> findRoleIdsByRoleNames(@Param("roleNames") Set<String> roleNames);
}
