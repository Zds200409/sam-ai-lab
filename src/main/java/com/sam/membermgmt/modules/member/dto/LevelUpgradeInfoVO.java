package com.sam.membermgmt.modules.member.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员升级预览信息（门户端展示用）。
 */
@Data
public class LevelUpgradeInfoVO {

    private Long currentLevelId;

    private String currentLevelName;

    private Long nextLevelId;

    private String nextLevelName;

    /** 补差价金额（next - current 的年费差） */
    private BigDecimal diffAmount;

    private boolean upgradable;
}
