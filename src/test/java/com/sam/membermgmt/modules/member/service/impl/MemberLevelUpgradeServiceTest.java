package com.sam.membermgmt.modules.member.service.impl;

import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeDTO;
import com.sam.membermgmt.modules.member.dto.MemberLevelUpgradeVO;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.entity.MemberLevelChangeRecord;
import com.sam.membermgmt.modules.member.entity.MemberUpgradePayment;
import com.sam.membermgmt.modules.member.mapper.MemberLevelChangeRecordMapper;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.mapper.MemberUpgradePaymentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberLevelUpgradeServiceTest {

    @Mock
    private MemberMapper memberMapper;
    @Mock
    private MemberLevelMapper levelMapper;
    @Mock
    private MemberLevelChangeRecordMapper changeRecordMapper;
    @Mock
    private MemberUpgradePaymentMapper upgradePaymentMapper;

    @InjectMocks
    private MemberAuthServiceImpl authService;

    private MemberLevel platinum() {
        MemberLevel l = new MemberLevel();
        l.setId(2L);
        l.setLevelName("白金会员");
        l.setAnnualFee(new BigDecimal("260"));
        l.setSortOrder(2);
        return l;
    }

    private MemberLevel excellence() {
        MemberLevel l = new MemberLevel();
        l.setId(3L);
        l.setLevelName("卓越会员");
        l.setAnnualFee(new BigDecimal("680"));
        l.setSortOrder(3);
        return l;
    }

    @Test
    void upgrade_should_make_up_difference_of_420_and_write_payment() {
        Member member = new Member();
        member.setId(1L);
        member.setCurrentLevelId(2L);

        when(memberMapper.selectById(1L)).thenReturn(member);
        when(levelMapper.selectById(2L)).thenReturn(platinum());
        when(levelMapper.selectOne(any())).thenReturn(excellence());

        MemberLevelUpgradeDTO dto = new MemberLevelUpgradeDTO();

        MemberLevelUpgradeVO vo = authService.upgradeLevel(1L, dto);

        assertEquals(3L, vo.getLevelId());
        assertEquals(0, vo.getDiffAmount().compareTo(new BigDecimal("420")));
        assertNotNull(vo.getPaymentNo());

        ArgumentCaptor<Member> memberCap = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateById(memberCap.capture());
        assertEquals(3L, memberCap.getValue().getCurrentLevelId());

        verify(changeRecordMapper).insert(any(MemberLevelChangeRecord.class));

        ArgumentCaptor<MemberUpgradePayment> payCap = ArgumentCaptor.forClass(MemberUpgradePayment.class);
        verify(upgradePaymentMapper).insert(payCap.capture());
        assertEquals(0, payCap.getValue().getAmount().compareTo(new BigDecimal("420")));
        assertEquals("PAID", payCap.getValue().getStatus());
    }

    @Test
    void upgrade_when_already_top_level_should_reject() {
        Member member = new Member();
        member.setId(1L);
        member.setCurrentLevelId(3L);

        when(memberMapper.selectById(1L)).thenReturn(member);
        when(levelMapper.selectById(3L)).thenReturn(excellence());
        when(levelMapper.selectOne(any())).thenReturn(null);

        MemberLevelUpgradeDTO dto = new MemberLevelUpgradeDTO();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.upgradeLevel(1L, dto));
        assertTrue(ex.getMessage().contains("最高"));
        verify(upgradePaymentMapper, never()).insert(any(MemberUpgradePayment.class));
    }
}
