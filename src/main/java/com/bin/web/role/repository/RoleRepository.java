package com.bin.web.role.repository;

import com.bin.web.role.entity.Role;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, String> {

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.roleId = :roleId")
    Optional<Role> findByRoleId(@Param("roleId") String roleId);

    @Query("SELECT DISTINCT r FROM Role r JOIN FETCH r.permissions WHERE r.roleId IN :roleIds")
    List<Role> findAllWithPermissionsByRoleIdIn(@Param("roleIds") List<String> roleIds);

    Optional<Role> findByRoleNameIgnoreCase(String roleName);

    @Query("SELECT r.roleId FROM Role r WHERE r.roleName IN :roleNames")
    Set<String> findRoleIdsByRoleNames(@Param("roleNames") Set<String> roleNames);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "INSERT INTO role_permissions (role_id , permission_id) VALUES (:roleId, :permissionId)",
            nativeQuery = true)
    void insertRolePermission(@Param("roleId") String roleId, @Param("permissionId") String permissionId);

    /**
     * 역할 권한 전체 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM role_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    int deleteRolePermissionsByPermissionId(@Param("permissionId") String permissionId);

    /**
     * 역할 권한 개별 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId",
            nativeQuery = true)
    void deleteRolePermission(@Param("roleId") String roleId, @Param("permissionId") String permissionId);

    @Transactional
    @Query(value = "SELECT COUNT(*) > 0 FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId",
            nativeQuery = true)
    boolean existsRolePermission(@Param("roleId") String roleId, @Param("permissionId") String permissionId);
}
