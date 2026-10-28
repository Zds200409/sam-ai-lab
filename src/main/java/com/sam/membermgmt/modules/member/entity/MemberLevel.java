package com.sam.membermgmt.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_level")
public class MemberLevel extends BaseEntity {
    private String levelName;
    private Integer levelCode;
    private BigDecimal pointsRate;
    private BigDecimal discountRate;
    private BigDecimal annualFee;
    private Integer sortOrder;
}
