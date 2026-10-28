package com.sam.membermgmt.modules.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberPointsRecord;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.mapper.MemberPointsRecordMapper;
import com.sam.membermgmt.modules.member.service.IPointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsServiceImpl extends ServiceImpl<MemberPointsRecordMapper, MemberPointsRecord> implements IPointsService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberPointsRecordMapper pointsRecordMapper;

    @Override
    @Transactional
    public void earnPoints(Long memberId, Long orderId, int points, String description) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        int balanceBefore = member.getTotalPoints();
        int balanceAfter = balanceBefore + points;
        member.setTotalPoints(balanceAfter);
        member.setAccumulatedPoints(member.getAccumulatedPoints() + points);
        memberMapper.updateById(member);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(memberId);
        record.setOrderId(orderId);
        record.setPoints(points);
        record.setType(1);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setDescription(description);
        pointsRecordMapper.insert(record);
    }

    @Override
    @Transactional
    public void consumePoints(Long memberId, Long orderId, int points, String description) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (member.getTotalPoints() < points) {
            throw new BusinessException("积分不足");
        }
        int balanceBefore = member.getTotalPoints();
        int balanceAfter = balanceBefore - points;
        member.setTotalPoints(balanceAfter);
        memberMapper.updateById(member);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(memberId);
        record.setOrderId(orderId);
        record.setPoints(-points);
        record.setType(3);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setDescription(description);
        pointsRecordMapper.insert(record);
    }

    @Override
    @Transactional
    public void manualAdjust(Long memberId, int points, String description) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        int balanceBefore = member.getTotalPoints();
        int balanceAfter = balanceBefore + points;
        if (balanceAfter < 0) {
            throw new BusinessException("积分不足，无法扣减");
        }
        member.setTotalPoints(balanceAfter);
        memberMapper.updateById(member);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(memberId);
        record.setPoints(points);
        record.setType(5);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setDescription(description);
        pointsRecordMapper.insert(record);
    }
}
