package com.bb.ballBin.role.service;

import com.bb.ballBin.board.service.BoardService;
import com.bb.ballBin.common.exception.NotFoundException;
import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.role.entity.Role;
import com.bb.ballBin.role.mapper.RoleMapper;
import com.bb.ballBin.role.model.RoleRequestDto;
import com.bb.ballBin.role.model.RoleResponseDto;
import com.bb.ballBin.role.repository.RoleRepository;
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
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    private final RoleMapper roleMapper;
    private final RoleRepository roleRepository;

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
}
