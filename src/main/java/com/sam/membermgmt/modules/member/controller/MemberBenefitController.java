package com.sam.membermgmt.modules.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.modules.member.entity.MemberBenefit;
import com.sam.membermgmt.modules.member.mapper.MemberBenefitMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "会员权益管理")
@RestController
@RequestMapping("/api/benefits")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class MemberBenefitController {

    @Autowired
    private MemberBenefitMapper benefitMapper;

    @Operation(summary = "按等级查询权益")
    @GetMapping("/list")
    public Result<List<MemberBenefit>> list(@RequestParam(required = false) Long levelId) {
        LambdaQueryWrapper<MemberBenefit> wrapper = new LambdaQueryWrapper<MemberBenefit>()
                .eq(levelId != null, MemberBenefit::getLevelId, levelId);
        return Result.success(benefitMapper.selectList(wrapper));
    }

    @OperationLog("新增权益")
    @Operation(summary = "新增权益")
    @PostMapping
    public Result<Void> save(@RequestBody MemberBenefit benefit) {
        benefitMapper.insert(benefit);
        return Result.success();
    }

    @OperationLog("编辑权益")
    @Operation(summary = "编辑权益")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MemberBenefit benefit) {
        benefit.setId(id);
        benefitMapper.updateById(benefit);
        return Result.success();
    }

    @OperationLog("删除权益")
    @Operation(summary = "删除权益")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        benefitMapper.deleteById(id);
        return Result.success();
    }
}
