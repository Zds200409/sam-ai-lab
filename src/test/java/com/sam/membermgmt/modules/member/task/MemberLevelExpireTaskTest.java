package com.sam.membermgmt.modules.member.task;

import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.entity.MemberLevelChangeRecord;
import com.sam.membermgmt.modules.member.mapper.MemberLevelChangeRecordMapper;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberLevelExpireTaskTest {

    @Mock
    private MemberMapper memberMapper;
    @Mock
    private MemberLevelMapper levelMapper;
    @Mock
    private MemberLevelChangeRecordMapper changeRecordMapper;

    @InjectMocks
    private MemberLevelExpireTask task;

    private MemberLevel lowest() {
        MemberLevel l = new MemberLevel();
        l.setId(2L);
        l.setLevelName("白金会员");
        l.setSortOrder(2);
        return l;
    }

    @Test
    void expired_high_level_member_should_be_downgraded_to_lowest_with_system_operator() {
        when(levelMapper.selectOne(any())).thenReturn(lowest());

        Member member = new Member();
        member.setId(10L);
        member.setCurrentLevelId(3L);
        when(memberMapper.selectList(any())).thenReturn(Collections.singletonList(member));

        task.downgradeExpiredMembers();

        ArgumentCaptor<Member> cap = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateById(cap.capture());
        assertEquals(2L, cap.getValue().getCurrentLevelId());

        ArgumentCaptor<MemberLevelChangeRecord> recCap = ArgumentCaptor.forClass(MemberLevelChangeRecord.class);
        verify(changeRecordMapper).insert(recCap.capture());
        MemberLevelChangeRecord rec = recCap.getValue();
        assertEquals(0L, rec.getOperatorId());
        assertEquals(2L, rec.getToLevelId());
        assertTrue(rec.getChangeReason().contains("自动降级"));
    }

    @Test
    void already_lowest_expired_member_should_not_be_downgraded() {
        when(levelMapper.selectOne(any())).thenReturn(lowest());
        when(memberMapper.selectList(any())).thenReturn(Collections.emptyList());

        task.downgradeExpiredMembers();

        verify(memberMapper, never()).updateById(any());
        verify(changeRecordMapper, never()).insert(any());
    }
}
