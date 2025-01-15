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
    public List<RoleResponseDto> roleList(){

        return roleService.getAllRoles();
    }

    @GetMapping("/{id}")
    @Operation(summary = "역할 ID 조회")
    public RoleResponseDto roleDetail(@PathVariable String id){

        return roleService.getRoleById(id);
    }

    @PostMapping("")
    @Operation(summary = "역할 생성")
    public ResponseEntity<String> addRole(@RequestBody RoleRequestDto roleRequestDto){

        roleService.createRole(roleRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("역할 생성에 성공하셨습니다.");
    }

    @PutMapping("/{id}")
    @Operation(summary = "역할 수정")
    ResponseEntity<String> modifyRole(@PathVariable String id, @RequestBody RoleRequestDto roleRequestDto){

        roleService.updateRole(id ,roleRequestDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("역할 수정에 성공하셨습니다.");
    }

    @DeleteMapping("")
    @Operation(summary = "역할 삭제")
    ResponseEntity<String> removeRole(@PathVariable String id){

        roleService.deleteRole(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("역할 삭제에 성공하셨습니다.");
    }
}
