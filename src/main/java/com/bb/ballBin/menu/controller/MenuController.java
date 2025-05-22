package com.bb.ballBin.menu.controller;

import com.bb.ballBin.common.annotation.MessageKey;
import com.bb.ballBin.menu.model.MenuRequest;
import com.bb.ballBin.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

//    @GetMapping
//    @Operation(summary = "메뉴 목록 조회")
//    public ResponseEntity<List<MenuResponse>> selectMenuList() {
//
//        return ResponseEntity.ok(menuService.getAllMenus());
//    }
//
//    @GetMapping("/{menuId}")
//    @Operation(summary = "메뉴 단일 조회")
//    public ResponseEntity<MenuResponse> selectMenuDetail(@PathVariable("menuId") String menuId) {
//
//        return ResponseEntity.ok(menuService.getMenuById(menuId));
//    }

    @PostMapping
    @Operation(summary = "메뉴 생성")
    @MessageKey(value = "success.menu.create")
    public ResponseEntity<String> createMenu(@RequestBody MenuRequest menuRequest) {

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{menuId}")
    @Operation(summary = "메뉴 수정")
    @MessageKey(value = "success.menu.update")
    public ResponseEntity<String> updateMenu(@PathVariable("menuId") String menuId, @RequestBody MenuRequest menuRequest) {

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{menuId}")
    @Operation(summary = "메뉴 삭제")
    @MessageKey(value = "success.menu.delete")
    public ResponseEntity<String> deleteMenu(@PathVariable("menuId") String menuId) {

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

