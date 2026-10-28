package com.sam.membermgmt.modules.member.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员升级（补差价）请求。
 * 不传 targetLevelId 时自动取当前等级的更高一级；
 * paidAmount 可选，传了则与服务端计算的补差价校验一致。
 */
@Data
public class MemberLevelUpgradeDTO {

    private Long targetLevelId;

    private BigDecimal paidAmount;

    /** 支付方式，如 到店支付 / 微信 / 支付宝 */
    private String paymentMethod;
}
