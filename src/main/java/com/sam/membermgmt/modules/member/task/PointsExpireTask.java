package com.sam.membermgmt.modules.member.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.enums.MemberStatusEnum;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.service.IPointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 积分过期作废任务。
 *
 * 每日凌晨执行：会员卡失效超过 90 天、仍未续费的，已累积积分自动作废。
 *
 * 积分本身不设有效期，只在会员卡失效后进入 90 天保留期。
 * 这样的规则可以用聚合余额实现；如果要支持「每笔积分入账满一年过期」，
 * 必须引入积分批次表和先进先出扣减，当前的聚合余额模型做不到。
 */
@Slf4j
@Component
public class PointsExpireTask {

    /** 会员卡失效后积分保留天数 */
    private static final int RETENTION_DAYS = 90;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private IPointsService pointsService;

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void expirePointsOfLongExpiredMembers() {
        LocalDate deadline = LocalDate.now().minusDays(RETENTION_DAYS);

        List<Member> targets = memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getStatus, MemberStatusEnum.EXPIRED.getCode())
                        .lt(Member::getExpiryDate, deadline)
                        .gt(Member::getTotalPoints, 0));

        if (targets.isEmpty()) {
            log.info("[POINTS-EXPIRE] 无需要作废积分的会员");
            return;
        }

        for (Member member : targets) {
            pointsService.expirePoints(member.getId(), member.getTotalPoints(),
                    "会员卡失效超过 " + RETENTION_DAYS + " 天，积分自动作废");
        }
        log.info("[POINTS-EXPIRE] 共作废 {} 名会员的积分", targets.size());
    }
}
