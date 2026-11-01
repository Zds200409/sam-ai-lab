package com.sam.membermgmt.modules.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberPointsRecord;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.mapper.MemberPointsRecordMapper;
import com.sam.membermgmt.modules.member.service.IPointsService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 积分过期作废的集成测试。
 *
 * 验证两件事：余额被清零、流水写入类型 4（过期扣除）。
 * 类级 @Transactional，测试结束回滚，不会真的把种子数据的积分扣掉。
 */
@SpringBootTest
@Transactional
class PointsExpireServiceTest {

    @Autowired
    private IPointsService pointsService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberPointsRecordMapper recordMapper;

    @Test
    void expirePointsZeroesBalanceAndWritesExpireRecord() {
        Member member = memberMapper.selectList(null).stream()
                .filter(m -> m.getTotalPoints() != null && m.getTotalPoints() > 0)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(member != null, "缺少有积分余额的会员种子数据");

        int before = member.getTotalPoints();
        int accumulatedBefore = member.getAccumulatedPoints() == null ? 0 : member.getAccumulatedPoints();

        pointsService.expirePoints(member.getId(), before, "单元测试：积分过期作废");

        Member after = memberMapper.selectById(member.getId());
        assertEquals(0, after.getTotalPoints(), "余额清零");
        assertEquals(accumulatedBefore, after.getAccumulatedPoints(), "历史累计获得不随支出变化");

        MemberPointsRecord rec = recordMapper.selectOne(
                new LambdaQueryWrapper<MemberPointsRecord>()
                        .eq(MemberPointsRecord::getMemberId, member.getId())
                        .eq(MemberPointsRecord::getType, 4)
                        .orderByDesc(MemberPointsRecord::getId)
                        .last("LIMIT 1"));

        assertNotNull(rec, "应写入 type=4 的过期扣除流水");
        assertEquals(-before, rec.getPoints(), "流水金额为负");
        assertEquals(before, rec.getBalanceBefore());
        assertEquals(0, rec.getBalanceAfter());
    }
}
