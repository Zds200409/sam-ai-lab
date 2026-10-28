package com.sam.membermgmt.modules.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.entity.MemberLevelChangeRecord;
import com.sam.membermgmt.modules.member.mapper.MemberLevelChangeRecordMapper;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.service.IMemberService;
import com.sam.membermgmt.utils.MemberNoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements IMemberService {

    @Autowired
    private MemberNoGenerator memberNoGenerator;

    @Autowired
    private MemberLevelMapper levelMapper;

    @Autowired
    private MemberLevelChangeRecordMapper changeRecordMapper;

    @Override
    @Transactional
    public void saveMember(Member member) {
        Member exist = getOne(new LambdaQueryWrapper<Member>().eq(Member::getPhone, member.getPhone()));
        if (exist != null) {
            throw new BusinessException("该手机号已注册为会员");
        }
        member.setMemberNo(memberNoGenerator.generate());
        member.setTotalConsumption(BigDecimal.ZERO);
        member.setTotalPoints(0);
        member.setAccumulatedPoints(0);
        member.setJoinDate(LocalDate.now());
        member.setExpiryDate(LocalDate.now().plusYears(1));
        member.setStatus(1);
        if (member.getCurrentLevelId() == null) {
            MemberLevel defaultLevel = levelMapper.selectOne(
                    new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSortOrder).last("LIMIT 1"));
            member.setCurrentLevelId(defaultLevel != null ? defaultLevel.getId() : 1L);
        }
        save(member);
    }

    @Override
    @Transactional
    public void updateMember(Member member) {
        Member exist = getById(member.getId());
        if (exist == null) {
            throw new BusinessException("会员不存在");
        }
        member.setMemberNo(null);
        member.setTotalConsumption(null);
        member.setTotalPoints(null);
        member.setAccumulatedPoints(null);
        updateById(member);
    }

    @Override
    @Transactional
    public void freeze(Long id) {
        Member member = getById(id);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (member.getStatus() != 1) {
            throw new BusinessException("只能冻结正常状态的会员");
        }
        member.setStatus(0);
        updateById(member);
    }

    @Override
    @Transactional
    public void unfreeze(Long id) {
        Member member = getById(id);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (member.getStatus() != 0) {
            throw new BusinessException("只能解冻已冻结的会员");
        }
        member.setStatus(1);
        updateById(member);
    }

    @Override
    @Transactional
    public void renew(Long id) {
        Member member = getById(id);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        MemberLevel level = levelMapper.selectById(member.getCurrentLevelId());
        member.setExpiryDate(LocalDate.now().plusYears(1));
        member.setStatus(1);
        updateById(member);
    }

    @Override
    public IPage<Member> pageWithLevel(Integer pageNum, Integer pageSize,
                                        String name, String phone, Long levelId, Integer status) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<Member>()
                .like(StringUtils.hasText(name), Member::getName, name)
                .like(StringUtils.hasText(phone), Member::getPhone, phone)
                .eq(levelId != null, Member::getCurrentLevelId, levelId)
                .eq(status != null, Member::getStatus, status)
                .orderByDesc(Member::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Member getMemberDetail(Long id) {
        Member member = getById(id);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (member.getCurrentLevelId() != null) {
            MemberLevel level = levelMapper.selectById(member.getCurrentLevelId());
            if (level != null) {
                member.setLevelName(level.getLevelName());
            }
        }
        return member;
    }
}
