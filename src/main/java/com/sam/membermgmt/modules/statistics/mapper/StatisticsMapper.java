package com.sam.membermgmt.modules.statistics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    Map<String, Object> selectTodayOrderStats();

    int selectTodayNewMembers();

    BigDecimal selectMonthAmount();

    int selectTotalMembers();

    int selectTotalProducts();

    int selectMonthPointsIssued();

    int selectPendingRenewals();

    List<Map<String, Object>> selectSalesTrend(@Param("days") int days);

    List<Map<String, Object>> selectMemberLevelDistribution();

    List<Map<String, Object>> selectTopProducts(@Param("limit") int limit);

    List<Map<String, Object>> selectSalesByCategory();

    List<Map<String, Object>> selectNewMembersByMonth();

    /** 今日生日会员 */
    List<Map<String, Object>> selectTodayBirthdays();

    /** 7天内到期会员 */
    List<Map<String, Object>> selectExpiringSoon();

    /** 按日期范围查询销售趋势 */
    List<Map<String, Object>> selectSalesTrendByRange(@Param("startDate") String startDate,
                                                      @Param("endDate") String endDate,
                                                      @Param("days") int days);

    /** 每日统计汇总写入（按 stat_date 幂等 upsert，供定时任务调用） */
    int upsertDailyStatistics(@Param("statDate") String statDate);
}
