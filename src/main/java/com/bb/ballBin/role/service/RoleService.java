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

    public List<RoleResponseDto> getAllRoles() {

        return roleRepository.findAll().stream()
                .map(Role::toDto)
                .collect(Collectors.toList());
    }

    public RoleResponseDto getRoleById(String id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("역할을 찾을 수 없습니다."));

        return role.toDto();
    }

    public void createRole(RoleRequestDto roleRequestDto) {

        if (roleRequestDto.getRoleName() == null || roleRequestDto.getRoleName().isEmpty()) {
            throw new IllegalArgumentException("역할 명은 필수 값 입니다.");
        }

        Role role = roleRequestDto.toEntity();
        roleRepository.save(role);
    }

    public void updateRole(String id, RoleRequestDto roleRequestDto) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("역할을 찾을 수 없습니다."));

        role.setRoleName(roleRequestDto.getRoleName());
        role.setRoleDescription(roleRequestDto.getRoleDescription());

        roleRepository.save(role);
    }

    public void deleteRole(String id) {

        roleRepository.deleteById(id);
    }
}
