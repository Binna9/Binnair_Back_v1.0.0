package com.bin.web.role.service;

import com.bin.web.common.exception.NotFoundException;
import com.bin.web.permission.entity.Permission;
import com.bin.web.permission.repository.PermissionRepository;
import com.bin.web.role.entity.Role;
import com.bin.web.role.mapper.RoleMapper;
import com.bin.web.role.model.RolePermissionRequestDto;
import com.bin.web.role.model.RoleRequestDto;
import com.bin.web.role.model.RoleResponseDto;
import com.bin.web.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    private final RoleMapper roleMapper;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 역할 전체 조회
     */
    public Page<RoleResponseDto> allRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(roleMapper::toDto);
    }

    /**
     * 역할 개별 조회
     */
    public RoleResponseDto roleById(String roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("error.role.notfound"));

        return roleMapper.toDto(role);
    }

    /**
     * 역할 생성
     */
    public void createRole(RoleRequestDto roleRequestDto) {
        try {
            if (roleRequestDto.getRoleName() == null || roleRequestDto.getRoleName().isEmpty()) {
                throw new RuntimeException("error.role.valid_role_name");
            }

            Role role = roleMapper.toEntity(roleRequestDto);

            roleRepository.save(role);

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 역할 수정
     */
    public void updateRole(String roleId, RoleRequestDto roleRequestDto) {
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new NotFoundException("error.role.notfound"));

            roleMapper.updateEntity(roleRequestDto, role);

            roleRepository.save(role);

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 역할 삭제
     */
    public void deleteRole(String roleId) {
        try {
            roleRepository.deleteById(roleId);
        } catch (Exception e) {
            logger.error("삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("삭제 중 오류 발생", e);
        }
    }

    /**
     * 역할 권한 조회
     */
    public Set<String> getPermissionsByRoles(Set<String> roleIds) {

        List<Role> roles = roleRepository.findAllWithPermissionsByRoleIdIn(List.copyOf(roleIds));

        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getPermissionName)
                .collect(Collectors.toSet());
    }

    /**
     * 역할 권한 부여
     */
    public void permissionToRole(List<RolePermissionRequestDto> rolePermissionRequestDtoList) {

        for(RolePermissionRequestDto dto : rolePermissionRequestDtoList) {

            Role role = roleRepository.findByRoleId(dto.getRoleId())
                    .orElseThrow(() -> new NotFoundException("error.role.notfound"));

            Permission permission = permissionRepository.findByPermissionName(dto.getPermissionName())
                    .orElseThrow(() -> new NotFoundException("error.permission.notfound"));

            boolean exists = roleRepository.existsRolePermission(role.getRoleId(), permission.getPermissionId());

            if(!exists){
                roleRepository.insertRolePermission(role.getRoleId(), permission.getPermissionId());
            }
        }
    }

    /**
     * 역할 권한 삭제
     */
    public void removeToRole(List<RolePermissionRequestDto> rolePermissionRequestDtoList) {

        for(RolePermissionRequestDto dto : rolePermissionRequestDtoList) {

            Role role = roleRepository.findByRoleId(dto.getRoleId())
                    .orElseThrow(() -> new NotFoundException("error.role.notfound"));

            Permission permission = permissionRepository.findByPermissionName(dto.getPermissionName())
                    .orElseThrow(() -> new NotFoundException("error.permission.notfound"));

            roleRepository.deleteRolePermission(role.getRoleId(), permission.getPermissionId());
        }
    }
}
