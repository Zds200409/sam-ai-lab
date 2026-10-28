package com.sam.membermgmt.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.modules.system.entity.SysUser;
import com.sam.membermgmt.modules.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequireRole({"ROLE_ADMIN"})
public class UserController {

    @Autowired
    private IUserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> page = userService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }

    @OperationLog("新增用户")
    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> save(@RequestBody SysUser user) {
        userService.saveUser(user);
        return Result.success();
    }

    @OperationLog("编辑用户")
    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success();
    }

    @OperationLog("删除用户")
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @OperationLog("重置密码")
    @Operation(summary = "重置密码")
    @PutMapping("/{id}/resetPwd")
    public Result<Void> resetPwd(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }

    @OperationLog("分配角色")
    @Operation(summary = "分配角色")
    @PutMapping("/{id}/assignRoles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody Long[] roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.success();
    }
}
