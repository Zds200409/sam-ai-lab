package com.sam.membermgmt.modules.member.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.entity.MemberLevelChangeRecord;
import com.sam.membermgmt.modules.member.mapper.MemberLevelChangeRecordMapper;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class MemberLevelExpireTask {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberLevelMapper levelMapper;

    @Autowired
    private MemberLevelChangeRecordMapper changeRecordMapper;

    /**
     * 每日凌晨 2 点执行：将会员有效期已过期且等级仍高于最低等级的会员自动降级至最低等级。
     * 最低等级由 sortOrder 最小者动态确定，不依赖具体 id 硬编码。
     * 已处于最低等级的过期会员不再重复降级，仅由前端 expired 标记提示续费。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void downgradeExpiredMembers() {
        MemberLevel lowest = levelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSortOrder).last("LIMIT 1"));
        if (lowest == null) {
            log.warn("会员等级表为空，跳过过期降级任务");
            return;
        }

        List<Member> expired = memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .lt(Member::getExpiryDate, LocalDate.now())
                        .eq(Member::getStatus, 1)
                        .gt(Member::getCurrentLevelId, lowest.getId()));

        if (expired.isEmpty()) {
            log.info("过期降级任务：无符合条件会员");
            return;
        }

        int count = 0;
        for (Member member : expired) {
            if (lowest.getId().equals(member.getCurrentLevelId())) {
                continue;
            }
            MemberLevel fromLevel = levelMapper.selectById(member.getCurrentLevelId());
            Long fromLevelId = member.getCurrentLevelId();

            member.setCurrentLevelId(lowest.getId());
            memberMapper.updateById(member);

            MemberLevelChangeRecord record = new MemberLevelChangeRecord();
            record.setMemberId(member.getId());
            record.setFromLevelId(fromLevelId);
            record.setToLevelId(lowest.getId());
            record.setChangeReason("会员有效期到期，系统自动降级："
                    + (fromLevel != null ? fromLevel.getLevelName() : "未知")
                    + " → " + lowest.getLevelName());
            record.setOperatorId(0L);
            changeRecordMapper.insert(record);
            count++;
        }
        log.info("过期降级任务完成：共降级 {} 名会员至 {}", count, lowest.getLevelName());
    }
}
