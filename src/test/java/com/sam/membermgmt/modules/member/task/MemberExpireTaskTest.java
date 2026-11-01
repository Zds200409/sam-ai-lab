package com.sam.membermgmt.modules.member.task;

import com.sam.membermgmt.common.enums.MemberStatusEnum;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberExpireTaskTest {

    @Mock
    private MemberMapper memberMapper;

    @InjectMocks
    private MemberExpireTask task;

    @Test
    void expired_member_should_be_marked_expired_without_changing_level() {
        Member member = new Member();
        member.setId(10L);
        member.setCurrentLevelId(3L);
        member.setStatus(MemberStatusEnum.ACTIVE.getCode());
        when(memberMapper.selectList(any())).thenReturn(List.of(member));

        task.expireMembers();

        ArgumentCaptor<Member> cap = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateById(cap.capture());
        Member updated = cap.getValue();
        assertEquals(MemberStatusEnum.EXPIRED.getCode(), updated.getStatus());
        assertEquals(3L, updated.getCurrentLevelId(), "过期只改状态，等级保留不降");
    }

    @Test
    void no_expired_member_should_not_update_anything() {
        when(memberMapper.selectList(any())).thenReturn(Collections.emptyList());

        task.expireMembers();

        verify(memberMapper, never()).updateById(any(Member.class));
    }
}
