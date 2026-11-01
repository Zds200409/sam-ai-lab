package com.sam.membermgmt.modules.member.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.enums.MemberStatusEnum;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 会员到期处理任务。
 *
 * 每日凌晨 2 点执行：把有效期已过、状态仍为「正常」的会员标记为「已过期」。
 *
 * 过期只改变会员状态，不改变会员等级。会员卡失效后无法下单结算，
 * 也不再享受等级折扣和积分倍率；续费后状态恢复为正常，原等级继续有效。
 * 等级不降，是因为等级由付费购买决定，与有效期无关，两者是独立的两个维度。
 */
@Slf4j
@Component
public class MemberExpireTask {

    @Autowired
    private MemberMapper memberMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void expireMembers() {
        List<Member> expired = memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .lt(Member::getExpiryDate, LocalDate.now())
                        .eq(Member::getStatus, MemberStatusEnum.ACTIVE.getCode()));

        if (expired.isEmpty()) {
            log.info("[MEMBER-EXPIRE] 无到期会员");
            return;
        }

        for (Member member : expired) {
            member.setStatus(MemberStatusEnum.EXPIRED.getCode());
            memberMapper.updateById(member);
        }
        log.info("[MEMBER-EXPIRE] 共标记 {} 名会员为已过期", expired.size());
    }
}
