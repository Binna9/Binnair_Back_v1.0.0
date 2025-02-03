package com.bb.ballBin.role.service;

import com.bb.ballBin.role.entity.Role;
import com.bb.ballBin.role.model.RoleRequestDto;
import com.bb.ballBin.role.model.RoleResponseDto;
import com.bb.ballBin.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository=roleRepository;
    }

    /**
     * 역할 전체 조회
     */
    public List<RoleResponseDto> getAllRoles() {

        return roleRepository.findAll().stream()
                .map(Role::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 역할 개별 조회
     */
    public RoleResponseDto getRoleById(String roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("error.role.notfound"));

        return role.toDto();
    }

    /**
     * 역할 생성
     */
    public void createRole(RoleRequestDto roleRequestDto) {

        if (roleRequestDto.getRoleName() == null || roleRequestDto.getRoleName().isEmpty()) {
            throw new IllegalArgumentException("error.role.valid_role_name");
        }

        Role role = roleRequestDto.toEntity();
        roleRepository.save(role);
    }

    /**
     * 역할 수정
     */
    public void updateRole(String roleId, RoleRequestDto roleRequestDto) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("error.role.notfound"));

        role.setRoleName(roleRequestDto.getRoleName());
        role.setRoleDescription(roleRequestDto.getRoleDescription());

        roleRepository.save(role);
    }

    /**
     * 역할 삭제
     */
    public void deleteRole(String roleId) {

        roleRepository.deleteById(roleId);
    }
}
