package com.sam.membermgmt.modules.statistics.controller;

import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.modules.statistics.dto.DashboardVO;
import com.sam.membermgmt.modules.statistics.mapper.StatisticsMapper;
import com.sam.membermgmt.modules.statistics.task.DailyStatisticsTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "数据统计")
@RestController
@RequestMapping("/api/statistics")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
public class StatisticsController {

    @Autowired
    private StatisticsMapper statisticsMapper;

    @Autowired
    private DailyStatisticsTask dailyStatisticsTask;

    @Operation(summary = "仪表盘总览")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        DashboardVO vo = new DashboardVO();

        Map<String, Object> todayStats = statisticsMapper.selectTodayOrderStats();
        vo.setTodayOrderCount(todayStats.get("orderCount") != null
                ? Integer.parseInt(todayStats.get("orderCount").toString()) : 0);
        vo.setTodayAmount(todayStats.get("totalAmount") != null
                ? new BigDecimal(todayStats.get("totalAmount").toString()) : BigDecimal.ZERO);

        vo.setTodayNewMembers(statisticsMapper.selectTodayNewMembers());
        vo.setMonthAmount(statisticsMapper.selectMonthAmount());
        vo.setTotalMembers(statisticsMapper.selectTotalMembers());
        vo.setTotalProducts(statisticsMapper.selectTotalProducts());
        vo.setPointsIssuedThisMonth(statisticsMapper.selectMonthPointsIssued());
        vo.setPendingRenewals(statisticsMapper.selectPendingRenewals());
        vo.setTodayBirthdays(statisticsMapper.selectTodayBirthdays());
        vo.setExpiringSoon(statisticsMapper.selectExpiringSoon());

        return Result.success(vo);
    }

    @Operation(summary = "销售趋势")
    @GetMapping("/sales-trend")
    public Result<List<Map<String, Object>>> salesTrend(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(statisticsMapper.selectSalesTrendByRange(startDate, endDate, days));
    }

    @Operation(summary = "会员分析")
    @GetMapping("/member-analysis")
    public Result<Map<String, Object>> memberAnalysis() {
        List<Map<String, Object>> levelDistribution = statisticsMapper.selectMemberLevelDistribution();
        List<Map<String, Object>> newMembersByMonth = statisticsMapper.selectNewMembersByMonth();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("levelDistribution", levelDistribution);
        result.put("newMembersByMonth", newMembersByMonth);
        return Result.success(result);
    }

    @Operation(summary = "热销商品排行")
    @GetMapping("/top-products")
    public Result<List<Map<String, Object>>> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(statisticsMapper.selectTopProducts(limit));
    }

    @Operation(summary = "分类销售占比")
    @GetMapping("/sales-by-category")
    public Result<List<Map<String, Object>>> salesByCategory() {
        return Result.success(statisticsMapper.selectSalesByCategory());
    }

    @OperationLog("手工汇总每日统计")
    @Operation(summary = "手工汇总每日统计（幂等，供补数/演示）")
    @PostMapping("/daily-summary")
    @RequireRole({"ROLE_ADMIN"})
    public Result<?> dailySummary(@RequestParam String date) {
        int rows = dailyStatisticsTask.summary(date);
        return Result.success("统计已汇总，date=" + date + "，影响行数=" + rows);
    }
}
