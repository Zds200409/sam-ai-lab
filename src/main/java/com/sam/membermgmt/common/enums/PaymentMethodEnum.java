package com.sam.membermgmt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethodEnum {
    CASH("CASH", "现金"),
    WECHAT("WECHAT", "微信支付"),
    ALIPAY("ALIPAY", "支付宝"),
    BANK_CARD("BANK_CARD", "银行卡");

    private final String code;
    private final String desc;
}
