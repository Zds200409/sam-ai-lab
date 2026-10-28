package com.sam.membermgmt.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.modules.system.entity.OperationLog;
import com.sam.membermgmt.modules.system.mapper.OperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询。
 * 写入侧由 common/aspect/OperationLogAspect.java 配合 @OperationLog 注解自动完成，
 * 本控制器提供后台"日志审计"所需的查询能力（对应论文 4.3.3 后台管理与 5.1.3 系统管理功能实现）。
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/operation-logs")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class OperationLogController {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Operation(summary = "操作日志分页查询")
    @GetMapping("/page")
    public Result<PageResult<OperationLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(username), OperationLog::getUsername, username)
                .like(StringUtils.hasText(operation), OperationLog::getOperation, operation)
                .eq(status != null, OperationLog::getStatus, status)
                .orderByDesc(OperationLog::getCreateTime);
        IPage<OperationLog> page = operationLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }
}
