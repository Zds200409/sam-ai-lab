package com.sam.membermgmt.modules.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    private Integer todayOrderCount;
    private BigDecimal todayAmount;
    private Integer todayNewMembers;
    private BigDecimal monthAmount;
    private Integer totalMembers;
    private Integer totalProducts;
    private Integer pointsIssuedThisMonth;
    private Integer pendingRenewals;
    /** 今日生日会员列表 */
    private List<Map<String, Object>> todayBirthdays;
    /** 7天内到期会员列表 */
    private List<Map<String, Object>> expiringSoon;
}
