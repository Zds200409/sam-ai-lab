package com.sam.membermgmt.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.modules.system.entity.Announcement;
import com.sam.membermgmt.modules.system.mapper.AnnouncementMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portal/announcements")
@Tag(name = "会员门户-公告")
public class MemberPortalAnnouncementController {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Operation(summary = "查看已发布门店公告")
    @GetMapping("/published")
    public Result<?> published() {
        return Result.success(announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .orderByDesc(Announcement::getCreateTime)));
    }
}
