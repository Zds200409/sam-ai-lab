package com.sam.membermgmt.modules.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.order.entity.ReturnRecord;
import com.sam.membermgmt.modules.order.mapper.ReturnRecordMapper;
import com.sam.membermgmt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/returns")
@Tag(name = "退换货管理")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
public class ReturnController {

    @Autowired
    private ReturnRecordMapper returnRecordMapper;

    @Operation(summary = "退换货分页")
    @GetMapping("/page")
    public Result<PageResult<ReturnRecord>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<ReturnRecord> wrapper = new LambdaQueryWrapper<ReturnRecord>()
                .eq(status != null, ReturnRecord::getStatus, status)
                .orderByDesc(ReturnRecord::getCreateTime);
        IPage<ReturnRecord> page = returnRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }

    @OperationLog("提交退换货申请")
    @Operation(summary = "提交退换货")
    @PostMapping
    public Result<?> save(@RequestBody ReturnRecord record) {
        record.setStatus(1);
        returnRecordMapper.insert(record);
        return Result.success("退换货申请已提交");
    }

    @OperationLog("审核通过退换货")
    @Operation(summary = "审核通过")
    @PutMapping("/{id}/approve")
    public Result<?> approve(@PathVariable Long id) {
        ReturnRecord record = returnRecordMapper.selectById(id);
        if (record == null || record.getStatus() != 1) throw new BusinessException("该申请当前不可审核");
        record.setStatus(2);
        record.setHandlerId(UserContext.getCurrentUserId());
        record.setHandleTime(LocalDateTime.now());
        returnRecordMapper.updateById(record);
        return Result.success("已通过");
    }

    @OperationLog("审核拒绝退换货")
    @Operation(summary = "审核拒绝")
    @PutMapping("/{id}/reject")
    public Result<?> reject(@PathVariable Long id) {
        ReturnRecord record = returnRecordMapper.selectById(id);
        if (record == null || record.getStatus() != 1) throw new BusinessException("该申请当前不可审核");
        record.setStatus(3);
        record.setHandlerId(UserContext.getCurrentUserId());
        record.setHandleTime(LocalDateTime.now());
        returnRecordMapper.updateById(record);
        return Result.success("已拒绝");
    }
}
