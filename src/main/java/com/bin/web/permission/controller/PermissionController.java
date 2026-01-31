package com.bin.web.permission.controller;

import com.bin.web.common.annotation.MessageKey;
import com.bin.web.permission.model.PermissionRequestDto;
import com.bin.web.permission.model.PermissionResponseDto;
import com.bin.web.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "권한 전체 조회")
    public ResponseEntity<Page<PermissionResponseDto>> getAllPermissions(
            @PageableDefault(page = 0, size = 8, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(permissionService.allPermissions(pageable));
    }

    @GetMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "권한 개별 조회")
    public ResponseEntity<PermissionResponseDto> getPermissionById(@PathVariable("permissionId") String permissionId){
        return ResponseEntity.ok(permissionService.permissionById(permissionId));
    }

    @PostMapping("")
    @Operation(summary = "권한 생성")
    @MessageKey(value = "success.permission.create")
    public ResponseEntity<Void> addPermission(@RequestBody PermissionRequestDto permissionRequestDto){

        permissionService.createPermission(permissionRequestDto);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    @Operation(summary = "권한 수정")
    @MessageKey(value = "success.permission.update")
    public ResponseEntity<Void> modifyPermission(@PathVariable("permissionId") String permissionId, @RequestBody PermissionRequestDto permissionRequestDto){

        permissionService.updatePermission(permissionId ,permissionRequestDto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    @Operation(summary = "권한 삭제")
    @MessageKey(value = "success.permission.delete")
    public ResponseEntity<Void> removePermission(@PathVariable("permissionId") String permissionId){

        permissionService.deletePermission(permissionId);

        return ResponseEntity.ok().build();
    }
}
