package com.sam.membermgmt.modules.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sam.membermgmt.modules.member.entity.MemberPointsRecord;

public interface IPointsService extends IService<MemberPointsRecord> {
    void earnPoints(Long memberId, Long orderId, int points, String description);
    void consumePoints(Long memberId, Long orderId, int points, String description);
    void manualAdjust(Long memberId, int points, String description);
}
