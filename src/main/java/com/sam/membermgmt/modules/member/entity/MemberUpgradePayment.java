package com.sam.membermgmt.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员升级支付流水。
 * 会员白金→卓越补差价时落一笔独立支付单，与等级变更记录关联，实现财务可审计闭环。
 */
@Data
@TableName("member_upgrade_payment")
public class MemberUpgradePayment {

    private Long id;

    /** 支付单号，格式 UP + 时间戳 */
    private String paymentNo;

    private Long memberId;

    /** 关联的等级变更记录 ID */
    private Long levelChangeRecordId;

    private Long fromLevelId;

    private Long toLevelId;

    /** 补差价金额 */
    private BigDecimal amount;

    /** 支付方式 */
    private String paymentMethod;

    /** 支付状态：PAID 已支付 */
    private String status;

    private LocalDateTime paidTime;

    private LocalDateTime createTime;
}
