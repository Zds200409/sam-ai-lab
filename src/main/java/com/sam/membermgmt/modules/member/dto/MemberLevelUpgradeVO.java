package com.sam.membermgmt.modules.member.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员升级结果：升级后的等级与本次补差价金额。
 */
@Data
public class MemberLevelUpgradeVO {

    private Long levelId;

    private String levelName;

    /** 本次补差价金额 */
    private BigDecimal diffAmount;

    /** 变更说明 */
    private String changeReason;

    /** 支付单号 */
    private String paymentNo;
}
