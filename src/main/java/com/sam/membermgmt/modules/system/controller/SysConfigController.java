package com.sam.membermgmt.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.system.entity.SysConfig;
import com.sam.membermgmt.modules.system.mapper.SysConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 基础参数配置。对应论文 3.2.3「基础配置则应覆盖门店信息、积分规则、消费等级、公告参数」，
 * 以及 4.3.3 后台管理模块的「基础参数维护」。
 */
@Tag(name = "参数配置")
@RestController
@RequestMapping("/api/configs")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class SysConfigController {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Operation(summary = "参数列表")
    @GetMapping("/list")
    public Result<List<SysConfig>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .and(StringUtils.hasText(keyword),
                     w -> w.like(SysConfig::getConfigKey, keyword)
                           .or().like(SysConfig::getConfigName, keyword))
                .orderByAsc(SysConfig::getId);
        return Result.success(sysConfigMapper.selectList(wrapper));
    }

    @Operation(summary = "按参数键取值")
    @GetMapping("/value/{configKey}")
    public Result<String> value(@PathVariable String configKey) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configKey));
        return Result.success(config == null ? null : config.getConfigValue());
    }

    @OperationLog("修改系统参数")
    @Operation(summary = "修改参数")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody SysConfig config) {
        SysConfig exist = sysConfigMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("参数项不存在");
        }
        if (StringUtils.hasText(config.getConfigValue())) {
            exist.setConfigValue(config.getConfigValue());
        }
        if (StringUtils.hasText(config.getConfigName())) {
            exist.setConfigName(config.getConfigName());
        }
        if (config.getRemark() != null) {
            exist.setRemark(config.getRemark());
        }
        sysConfigMapper.updateById(exist);
        return Result.success("参数已保存");
    }
}
