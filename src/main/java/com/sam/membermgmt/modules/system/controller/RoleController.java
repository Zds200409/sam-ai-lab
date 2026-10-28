package com.sam.membermgmt.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.modules.system.entity.SysRole;
import com.sam.membermgmt.modules.system.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/roles")
@RequireRole({"ROLE_ADMIN"})
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @Operation(summary = "角色列表")
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<SysRole> page = roleService.page(new Page<>(pageNum, pageSize));
        return Result.success(PageResult.of(page));
    }

    @OperationLog("新增角色")
    @Operation(summary = "新增角色")
    @PostMapping
    public Result<Void> save(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.success();
    }

    @OperationLog("编辑角色")
    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateById(role);
        return Result.success();
    }

    @OperationLog("删除角色")
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "查询角色已分配的菜单")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> menus(@PathVariable Long id) {
        return Result.success(roleService.getMenuIds(id));
    }

    @OperationLog("分配菜单权限")
    @Operation(summary = "分配菜单权限")
    @PutMapping("/{id}/assignMenus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody Long[] menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }
}
