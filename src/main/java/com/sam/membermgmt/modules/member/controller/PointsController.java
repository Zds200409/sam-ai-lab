package com.sam.membermgmt.modules.member.controller;

import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.modules.member.service.IPointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "积分管理")
@RestController
@RequestMapping("/api/points")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER"})
public class PointsController {

    @Autowired
    private IPointsService pointsService;

    @OperationLog("手动调整积分")
    @Operation(summary = "手动调整积分")
    @PostMapping("/adjust")
    public Result<Void> adjust(@RequestBody Map<String, Object> params) {
        Long memberId = Long.valueOf(params.get("memberId").toString());
        int points = Integer.parseInt(params.get("points").toString());
        String description = (String) params.getOrDefault("description", "人工调整");
        pointsService.manualAdjust(memberId, points, description);
        return Result.success();
    }
}
