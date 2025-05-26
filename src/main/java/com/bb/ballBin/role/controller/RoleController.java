package com.bb.ballBin.role.controller;

import com.bb.ballBin.common.annotation.CurrentUserId;
import com.bb.ballBin.common.annotation.MessageKey;
import com.bb.ballBin.role.model.RolePermissionRequestDto;
import com.bb.ballBin.role.model.RoleRequestDto;
import com.bb.ballBin.role.model.RoleResponseDto;
import com.bb.ballBin.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("")
    @Operation(summary = "역할 전체 조회")
    public ResponseEntity<Page<RoleResponseDto>> getAllRoles(
            @PageableDefault(page = 0, size = 8, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(roleService.allRoles(pageable));
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "역할 개별 조회")
    public ResponseEntity<RoleResponseDto> getRoleById(@PathVariable("roleId") String roleId){
        return ResponseEntity.ok(roleService.roleById(roleId));
    }

    @PostMapping("")
    @Operation(summary = "역할 생성")
    @MessageKey(value = "success.role.create")
    public ResponseEntity<Void> addRole(@RequestBody RoleRequestDto roleRequestDto){

        roleService.createRole(roleRequestDto);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "역할 수정")
    @MessageKey(value = "success.role.update")
    public ResponseEntity<Void> modifyRole(@PathVariable("roleId") String roleId, @RequestBody RoleRequestDto roleRequestDto){

        roleService.updateRole(roleId ,roleRequestDto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "역할 삭제")
    @MessageKey(value = "success.role.permission.remove")
    public ResponseEntity<Void> removeRole(@PathVariable("roleId") String roleId){

        roleService.deleteRole(roleId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/permission")
    @Operation(summary = "역할 권한 조회")
    public ResponseEntity<Set<String>> getRolePermissions(@RequestParam("roleIds") Set<String> roleIds) {

        Set<String> permissions = roleService.getPermissionsByRoles(roleIds);

        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/assign-permission")
    @Operation(summary = "역할 권한 부여")
    @MessageKey(value = "success.role.permission.assign")
    public ResponseEntity<Void> assignPermissionToRole(@RequestBody RolePermissionRequestDto rolePermissionRequestDto) {

        roleService.permissionToRole(rolePermissionRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/remove-permission")
    @Operation(summary = "역할 권한 삭제")
    @MessageKey(value = "success.role.permission.remove")
    public ResponseEntity<Void> removePermissionToRole(@RequestBody RolePermissionRequestDto rolePermissionRequestDto) {

        roleService.removeToRole(rolePermissionRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
