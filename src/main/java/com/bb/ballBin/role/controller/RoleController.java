package com.bb.ballBin.role.controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.role.model.RoleRequestDto;
import com.bb.ballBin.role.model.RoleResponseDto;
import com.bb.ballBin.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("")
    @Operation(summary = "역할 전체 조회")
    public ResponseEntity<List<RoleResponseDto>> roleList(){

        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "역할 개별 조회")
    public ResponseEntity<RoleResponseDto> roleDetail(@PathVariable("roleId") String roleId){

        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }

    @PostMapping("")
    @Operation(summary = "역할 생성")
    @MessageKey(value = "success.role.create")
    public ResponseEntity<String> addRole(@RequestBody RoleRequestDto roleRequestDto){

        roleService.createRole(roleRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "역할 수정")
    @MessageKey(value = "success.role.update")
    public ResponseEntity<String> modifyRole(@PathVariable("roleId") String roleId, @RequestBody RoleRequestDto roleRequestDto){

        roleService.updateRole(roleId ,roleRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "역할 삭제")
    @MessageKey(value = "success.role.delete")
    public ResponseEntity<String> removeRole(@PathVariable("roleId") String roleId){

        roleService.deleteRole(roleId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
