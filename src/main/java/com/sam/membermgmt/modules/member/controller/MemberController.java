package com.sam.membermgmt.modules.member.controller;

import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevelChangeRecord;
import com.sam.membermgmt.modules.member.entity.MemberPointsRecord;
import com.sam.membermgmt.modules.member.mapper.MemberLevelChangeRecordMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.mapper.MemberPointsRecordMapper;
import com.sam.membermgmt.modules.member.service.IMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

@Tag(name = "会员管理")
@RestController
@RequestMapping("/api/members")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class MemberController {

    @Autowired
    private IMemberService memberService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberPointsRecordMapper pointsRecordMapper;

    @Autowired
    private MemberLevelChangeRecordMapper changeRecordMapper;

    @Operation(summary = "分页查询会员")
    @GetMapping("/page")
    @RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
    public Result<PageResult<Member>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) Integer status) {
        IPage<Member> page = memberService.pageWithLevel(pageNum, pageSize, name, phone, levelId, status);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "会员详情")
    @GetMapping("/{id}")
    @RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
    public Result<Member> detail(@PathVariable Long id) {
        return Result.success(memberService.getMemberDetail(id));
    }

    @OperationLog("新增会员")
    @Operation(summary = "新增会员")
    @PostMapping
    public Result<Void> save(@RequestBody Member member) {
        memberService.saveMember(member);
        return Result.success();
    }

    @OperationLog("编辑会员")
    @Operation(summary = "编辑会员")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Member member) {
        member.setId(id);
        memberService.updateMember(member);
        return Result.success();
    }

    @OperationLog("冻结会员")
    @Operation(summary = "冻结会员")
    @PostMapping("/{id}/freeze")
    public Result<Void> freeze(@PathVariable Long id) {
        memberService.freeze(id);
        return Result.success();
    }

    @OperationLog("解冻会员")
    @Operation(summary = "解冻会员")
    @PostMapping("/{id}/unfreeze")
    public Result<Void> unfreeze(@PathVariable Long id) {
        memberService.unfreeze(id);
        return Result.success();
    }

    @OperationLog("会员续费")
    @Operation(summary = "续费")
    @PostMapping("/{id}/renew")
    public Result<Void> renew(@PathVariable Long id) {
        memberService.renew(id);
        return Result.success();
    }

    @OperationLog("注销会员")
    @Operation(summary = "注销会员（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        Member member = memberService.getMemberDetail(id);
        if (member == null) {
            throw new BusinessException("会员不存在或已注销");
        }
        // @TableLogic 逻辑删除：仅置 deleted=1，会员的历史消费、积分与订单记录全部保留可追溯
        memberMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "积分流水")
    @GetMapping("/{id}/points-records")
    public Result<List<MemberPointsRecord>> pointsRecords(@PathVariable Long id) {
        List<MemberPointsRecord> records = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<MemberPointsRecord>()
                        .eq(MemberPointsRecord::getMemberId, id)
                        .orderByDesc(MemberPointsRecord::getCreateTime));
        return Result.success(records);
    }

    @Operation(summary = "等级变更记录")
    @GetMapping("/{id}/level-changes")
    public Result<List<MemberLevelChangeRecord>> levelChanges(@PathVariable Long id) {
        List<MemberLevelChangeRecord> records = changeRecordMapper.selectList(
                new LambdaQueryWrapper<MemberLevelChangeRecord>()
                        .eq(MemberLevelChangeRecord::getMemberId, id)
                        .orderByDesc(MemberLevelChangeRecord::getCreateTime));
        return Result.success(records);
    }
}
