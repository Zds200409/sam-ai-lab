package com.sam.membermgmt.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.system.entity.Announcement;
import com.sam.membermgmt.modules.system.mapper.AnnouncementMapper;
import com.sam.membermgmt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
@Tag(name = "公告管理")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class AnnouncementController {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Operation(summary = "公告分页")
    @GetMapping("/page")
    public Result<PageResult<Announcement>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(status != null, Announcement::getStatus, status)
                .orderByDesc(Announcement::getCreateTime);
        IPage<Announcement> page = announcementMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }

    @OperationLog("新增公告")
    @Operation(summary = "新增公告")
    @PostMapping
    public Result<?> save(@RequestBody Announcement announcement) {
        announcement.setPublisher(UserContext.getCurrentUsername());
        announcementMapper.insert(announcement);
        return Result.success("公告已创建");
    }

    @OperationLog("编辑公告")
    @Operation(summary = "编辑公告")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementMapper.updateById(announcement);
        return Result.success("公告已更新");
    }

    @OperationLog("发布或撤回公告")
    @Operation(summary = "发布或撤回公告")
    @PutMapping("/{id}/publish")
    public Result<?> publish(@PathVariable Long id, @RequestParam Integer status) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) throw new BusinessException("公告不存在");
        announcement.setStatus(status);
        announcementMapper.updateById(announcement);
        return Result.success(status == 1 ? "已发布" : "已撤回");
    }

    @OperationLog("删除公告")
    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) throw new BusinessException("公告不存在");
        announcementMapper.deleteById(id);
        return Result.success("公告已删除");
    }
}
