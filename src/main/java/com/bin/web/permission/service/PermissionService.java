package com.bin.web.permission.service;

import com.bin.web.common.exception.NotFoundException;
import com.bin.web.permission.entity.Permission;
import com.bin.web.permission.mapper.PermissionMapper;
import com.bin.web.permission.model.PermissionRequestDto;
import com.bin.web.permission.model.PermissionResponseDto;
import com.bin.web.permission.repository.PermissionRepository;
import com.bin.web.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    /**
     * 권한 전체 조회
     */
    public Page<PermissionResponseDto> allPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable).map(permissionMapper::toDto);
    }

    /**
     * 권한 개별 조회
     */
    public PermissionResponseDto permissionById(String permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("error.permission.notfound"));
        return permissionMapper.toDto(permission);
    }

    /**
     * 권한 생성
     */
    public void createPermission(PermissionRequestDto permissionRequestDto) {
        try {
            if (permissionRequestDto.getPermissionName() == null || permissionRequestDto.getPermissionName().isEmpty()) {
                throw new RuntimeException("error.permission.valid_permission_name");
            }

            Permission permission = permissionMapper.toEntity(permissionRequestDto);
            permissionRepository.save(permission);

        } catch (Exception e) {
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 권한 수정
     */
    public void updatePermission(String permissionId, PermissionRequestDto permissionRequestDto) {
        try {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new NotFoundException("error.permission.notfound"));

            permissionMapper.updateEntity(permissionRequestDto, permission);
            permissionRepository.save(permission);

        } catch (Exception e) {
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 권한 삭제
     */
    @Transactional
    public void deletePermission(String permissionId) {

        permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("error.permission.notfound"));

        roleRepository.deleteRolePermissionsByPermissionId(permissionId);
        permissionRepository.deleteById(permissionId);
    }
}
