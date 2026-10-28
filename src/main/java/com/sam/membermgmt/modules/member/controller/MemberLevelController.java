package com.sam.membermgmt.modules.member.controller;

import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

@Tag(name = "会员等级管理")
@RestController
@RequestMapping("/api/levels")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class MemberLevelController {

    @Autowired
    private MemberLevelMapper levelMapper;

    @Operation(summary = "等级列表")
    @GetMapping("/list")
    @RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
    public Result<List<MemberLevel>> list() {
        return Result.success(levelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSortOrder)));
    }

    @Operation(summary = "分页查询等级")
    @GetMapping("/page")
    public Result<PageResult<MemberLevel>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<MemberLevel> page = levelMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSortOrder));
        return Result.success(PageResult.of(page));
    }

    @OperationLog("新增等级")
    @Operation(summary = "新增等级")
    @PostMapping
    public Result<Void> save(@RequestBody MemberLevel level) {
        levelMapper.insert(level);
        return Result.success();
    }

    @OperationLog("编辑等级")
    @Operation(summary = "编辑等级")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MemberLevel level) {
        level.setId(id);
        levelMapper.updateById(level);
        return Result.success();
    }
}
