package com.sam.membermgmt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    COMPLETED(1, "已完成"),
    PENDING_REFUND(2, "待审核退款"),
    REFUNDED(3, "已退款"),
    REFUND_REJECTED(4, "退款已拒绝"),
    CANCELLED(5, "已作废");

    private final int code;
    private final String desc;
}
