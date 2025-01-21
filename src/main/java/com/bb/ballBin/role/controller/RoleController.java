package com.bb.ballBin.role.controller;

import com.bb.ballBin.role.model.RoleRequestDto;
import com.bb.ballBin.role.model.RoleResponseDto;
import com.bb.ballBin.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("")
    @Operation(summary = "역할 전체 조회")
    public ResponseEntity<List<RoleResponseDto>> roleList(){

        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "역할 개별 조회")
    public ResponseEntity<RoleResponseDto> roleDetail(@PathVariable String roleId){

        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }

    @PostMapping("")
    @Operation(summary = "역할 생성")
    public ResponseEntity<String> addRole(@RequestBody RoleRequestDto roleRequestDto){

        roleService.createRole(roleRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("역할 생성에 성공하셨습니다.");
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "역할 수정")
    public ResponseEntity<String> modifyRole(@PathVariable String roleId, @RequestBody RoleRequestDto roleRequestDto){

        roleService.updateRole(roleId ,roleRequestDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("역할 수정에 성공하셨습니다.");
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "역할 삭제")
    public ResponseEntity<String> removeRole(@PathVariable String roleId){

        roleService.deleteRole(roleId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("역할 삭제에 성공하셨습니다.");
    }
}
