package com.sam.membermgmt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PointsTypeEnum {
    EARN_PURCHASE(1, "消费获得"),
    EARN_ACTIVITY(2, "活动获得"),
    CONSUME_EXCHANGE(3, "积分兑换"),
    EXPIRE(4, "过期扣除"),
    MANUAL_ADJUST(5, "人工调整");

    private final int code;
    private final String desc;
}
