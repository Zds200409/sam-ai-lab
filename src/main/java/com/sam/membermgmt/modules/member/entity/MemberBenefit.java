package com.sam.membermgmt.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_benefit")
public class MemberBenefit extends BaseEntity {
    private Long levelId;
    private String benefitName;
    private String benefitType;
    private String benefitValue;
    private Integer pointsCost;
    private String description;
    private Integer status;
}
