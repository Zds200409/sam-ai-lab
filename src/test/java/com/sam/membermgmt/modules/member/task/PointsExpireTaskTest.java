package com.sam.membermgmt.modules.member.task;

import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.service.IPointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsExpireTaskTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private IPointsService pointsService;

    @InjectMocks
    private PointsExpireTask task;

    @Test
    void longExpiredMemberPointsShouldBeExpired() {
        Member member = new Member();
        member.setId(10L);
        member.setTotalPoints(2380);
        when(memberMapper.selectList(any())).thenReturn(List.of(member));

        task.expirePointsOfLongExpiredMembers();

        verify(pointsService).expirePoints(eq(10L), eq(2380), anyString());
    }

    @Test
    void noTargetMemberShouldNotTouchPoints() {
        when(memberMapper.selectList(any())).thenReturn(Collections.emptyList());

        task.expirePointsOfLongExpiredMembers();

        verify(pointsService, never()).expirePoints(anyLong(), anyInt(), anyString());
    }
}
