package com.sam.membermgmt.modules.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeDTO;
import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeVO;
import com.sam.membermgmt.modules.member.dto.MemberLoginDTO;
import com.sam.membermgmt.modules.member.dto.MemberLoginVO;
import com.sam.membermgmt.modules.member.dto.MemberPasswordDTO;
import com.sam.membermgmt.modules.member.dto.MemberProfileDTO;
import com.sam.membermgmt.modules.member.dto.MemberRegisterDTO;
import com.sam.membermgmt.modules.member.dto.MemberResetPasswordDTO;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.entity.MemberLevelChangeRecord;
import com.sam.membermgmt.modules.member.entity.MemberUpgradePayment;
import com.sam.membermgmt.modules.member.mapper.MemberLevelChangeRecordMapper;
import com.sam.membermgmt.modules.member.mapper.MemberUpgradePaymentMapper;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.service.IMemberAuthService;
import com.sam.membermgmt.modules.system.service.CaptchaService;
import com.sam.membermgmt.utils.JwtUtil;
import com.sam.membermgmt.utils.MemberNoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class MemberAuthServiceImpl implements IMemberAuthService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberLevelMapper levelMapper;

    @Autowired
    private MemberLevelChangeRecordMapper changeRecordMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private MemberNoGenerator memberNoGenerator;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private MemberUpgradePaymentMapper upgradePaymentMapper;

    @Override
    @Transactional
    public MemberLoginVO register(MemberRegisterDTO dto) {
        Long exists = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>().eq(Member::getPhone, dto.getPhone()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该手机号已注册，请直接登录");
        }

        Member member = new Member();
        member.setPhone(dto.getPhone());
        member.setPassword(passwordEncoder.encode(dto.getPassword()));
        member.setName(dto.getName());
        member.setGender(dto.getGender());
        member.setBirthday(dto.getBirthday());
        member.setEmail(dto.getEmail());
        member.setMemberNo(memberNoGenerator.generate());
        member.setTotalConsumption(BigDecimal.ZERO);
        member.setTotalPoints(0);
        member.setAccumulatedPoints(0);
        member.setJoinDate(LocalDate.now());
        member.setExpiryDate(LocalDate.now().plusYears(1));
        member.setStatus(1);

        // 默认等级取 sortOrder 最小者（白金会员）
        MemberLevel defaultLevel = levelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSortOrder).last("LIMIT 1"));
        member.setCurrentLevelId(defaultLevel != null ? defaultLevel.getId() : 2L);
        memberMapper.insert(member);

        return buildVO(member, generateToken(member));
    }

    @Override
    public MemberLoginVO login(MemberLoginDTO dto) {
        if (!captchaService.validate(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getPhone, dto.getPhone()));
        if (member == null) {
            throw new BusinessException("手机号或密码错误");
        }
        if (member.getStatus() != null && member.getStatus() == 0) {
            throw new BusinessException("账号已被冻结，请联系门店");
        }
        if (!StringUtils.hasText(member.getPassword())) {
            throw new BusinessException("该会员尚未开通线上登录，请联系门店设置密码");
        }
        if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }
        return buildVO(member, generateToken(member));
    }

    @Override
    public MemberLoginVO getProfile(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return buildVO(member, null);
    }

    @Override
    @Transactional
    public void updateProfile(Long memberId, MemberProfileDTO dto) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (dto.getName() != null) {
            member.setName(dto.getName());
        }
        if (dto.getGender() != null) {
            member.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            member.setBirthday(dto.getBirthday());
        }
        if (dto.getEmail() != null) {
            member.setEmail(dto.getEmail());
        }
        memberMapper.updateById(member);
    }

    @Override
    @Transactional
    public void changePassword(Long memberId, MemberPasswordDTO dto) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (!StringUtils.hasText(member.getPassword())
                || !passwordEncoder.matches(dto.getOldPassword(), member.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        member.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        memberMapper.updateById(member);
    }

    @Override
    @Transactional
    public void resetPassword(MemberResetPasswordDTO dto) {
        if (!captchaService.validate(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getPhone, dto.getPhone()));
        if (member == null) {
            throw new BusinessException("该手机号尚未注册会员");
        }
        if (member.getStatus() != null && member.getStatus() == 0) {
            throw new BusinessException("账号已被冻结，请联系门店");
        }
        member.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        memberMapper.updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberLevelUpgradeVO upgradeLevel(Long memberId, MemberLevelUpgradeDTO dto) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        MemberLevel current = levelMapper.selectById(member.getCurrentLevelId());
        if (current == null) {
            throw new BusinessException("当前会员等级不存在");
        }

        // 目标等级：指定则取其值，否则取当前等级的更高一级
        MemberLevel target;
        if (dto.getTargetLevelId() != null) {
            target = levelMapper.selectById(dto.getTargetLevelId());
        } else {
            target = levelMapper.selectOne(
                    new LambdaQueryWrapper<MemberLevel>()
                            .gt(MemberLevel::getSortOrder, current.getSortOrder())
                            .orderByAsc(MemberLevel::getSortOrder)
                            .last("LIMIT 1"));
        }
        if (target == null) {
            throw new BusinessException("您已是最高的「" + current.getLevelName() + "」，无需升级");
        }
        if (target.getSortOrder() <= current.getSortOrder()) {
            throw new BusinessException("只能升级到更高等级");
        }

        // 补差价 = 目标年费 - 当前年费
        BigDecimal diff = target.getAnnualFee().subtract(current.getAnnualFee());
        if (diff.compareTo(BigDecimal.ZERO) < 0) {
            diff = BigDecimal.ZERO;
        }
        if (dto.getPaidAmount() != null && dto.getPaidAmount().compareTo(diff) != 0) {
            throw new BusinessException("补差价金额有误，应补 ¥" + diff
                    + "（" + current.getLevelName() + " → " + target.getLevelName() + "）");
        }

        // 1. 更新会员等级
        member.setCurrentLevelId(target.getId());
        memberMapper.updateById(member);

        // 2. 写入等级变更记录
        MemberLevelChangeRecord record = new MemberLevelChangeRecord();
        record.setMemberId(memberId);
        record.setFromLevelId(current.getId());
        record.setToLevelId(target.getId());
        record.setChangeReason("补差价升级：" + current.getLevelName() + " → " + target.getLevelName()
                + "，补差价 ¥" + diff);
        record.setOperatorId(memberId);
        changeRecordMapper.insert(record);

        // 3. 写入升级支付流水（补差价），与等级变更记录关联，实现可审计闭环
        String paymentNo = "UP" + System.currentTimeMillis();
        MemberUpgradePayment payment = new MemberUpgradePayment();
        payment.setPaymentNo(paymentNo);
        payment.setMemberId(memberId);
        payment.setLevelChangeRecordId(record.getId());
        payment.setFromLevelId(current.getId());
        payment.setToLevelId(target.getId());
        payment.setAmount(diff);
        payment.setPaymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod() : "到店支付");
        payment.setStatus("PAID");
        payment.setPaidTime(LocalDateTime.now());
        upgradePaymentMapper.insert(payment);

        MemberLevelUpgradeVO vo = new MemberLevelUpgradeVO();
        vo.setLevelId(target.getId());
        vo.setLevelName(target.getLevelName());
        vo.setDiffAmount(diff);
        vo.setChangeReason(record.getChangeReason());
        vo.setPaymentNo(paymentNo);
        return vo;
    }

    private String generateToken(Member member) {
        List<String> roles = Collections.singletonList("ROLE_MEMBER");
        return jwtUtil.generateMemberToken(member.getId(), member.getPhone(), roles);
    }

    private MemberLoginVO buildVO(Member member, String token) {
        boolean expired = member.getExpiryDate() != null
                && member.getExpiryDate().isBefore(LocalDate.now());
        String levelName = null;
        if (member.getCurrentLevelId() != null) {
            MemberLevel level = levelMapper.selectById(member.getCurrentLevelId());
            if (level != null) {
                levelName = level.getLevelName();
            }
        }
        return MemberLoginVO.builder()
                .token(token)
                .memberId(member.getId())
                .memberNo(member.getMemberNo())
                .name(member.getName())
                .phone(member.getPhone())
                .gender(member.getGender())
                .birthday(member.getBirthday())
                .email(member.getEmail())
                .levelId(member.getCurrentLevelId())
                .levelName(levelName)
                .totalPoints(member.getTotalPoints())
                .accumulatedPoints(member.getAccumulatedPoints())
                .totalConsumption(member.getTotalConsumption())
                .expiryDate(member.getExpiryDate())
                .expired(expired)
                .status(member.getStatus())
                .roles(Collections.singletonList("ROLE_MEMBER"))
                .build();
    }
}
