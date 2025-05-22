package com.bb.ballBin.permission.service;

import com.bb.ballBin.common.exception.NotFoundException;
import com.bb.ballBin.permission.entity.Permission;
import com.bb.ballBin.permission.mapper.PermissionMapper;
import com.bb.ballBin.permission.model.PermissionRequestDto;
import com.bb.ballBin.permission.model.PermissionResponseDto;
import com.bb.ballBin.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    private final PermissionMapper permissionMapper;
    private final PermissionRepository permissionRepository;

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
            logger.error("오류 발생: {}", e.getMessage(), e);
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
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 권한 삭제
     */
    public void deletePermission(String permissionId) {
        try {
            permissionRepository.deleteById(permissionId);
        } catch (Exception e) {
            logger.error("삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("삭제 중 오류 발생", e);
        }
    }
}
