package com.sam.membermgmt.modules.statistics.task;

import com.sam.membermgmt.modules.statistics.mapper.StatisticsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日统计汇总任务。
 * 每天凌晨汇总前一日的订单量、销售额、会员数、新增会员与积分发放/消耗，
 * 幂等写入 daily_statistics 表（依赖 uk_stat_date 唯一键），
 * 对应论文 4.2.3「每日统计表(daily_statistics)」与 4.3.5 统计分析模块。
 */
@Slf4j
@Component
public class DailyStatisticsTask {

    @Autowired
    private StatisticsMapper statisticsMapper;

    /** 每天 00:10:00 汇总前一日数据 */
    @Scheduled(cron = "0 10 0 * * ?")
    public void summaryYesterday() {
        summary(LocalDate.now().minusDays(1).toString());
    }

    /** 汇总指定日期（幂等，可重复执行；供定时任务与手工补数调用） */
    public int summary(String statDate) {
        try {
            int rows = statisticsMapper.upsertDailyStatistics(statDate);
            log.info("每日统计汇总完成：stat_date={}，影响 {} 行", statDate, rows);
            return rows;
        } catch (Exception e) {
            log.error("每日统计汇总失败：stat_date={}", statDate, e);
            return 0;
        }
    }
}
