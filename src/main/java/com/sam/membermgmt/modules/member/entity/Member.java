package com.sam.membermgmt.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member")
public class Member extends BaseEntity {
    private String memberNo;
    private String name;
    private String phone;
    private String password;
    private Integer gender;
    private LocalDate birthday;
    private String email;
    private String idCard;
    private Long currentLevelId;
    private BigDecimal totalConsumption;
    private Integer totalPoints;
    private Integer accumulatedPoints;
    private LocalDate joinDate;
    private LocalDate expiryDate;
    private Integer status;
    private String remark;

    /** 逻辑删除标记：0 未删除，1 已删除（MyBatis-Plus 查询自动过滤，保证注销会员的历史消费记录仍可追溯） */
    @TableLogic
    private Integer deleted;

    /** 当前等级名称（非数据库字段，查询时填充，便于前端展示） */
    @TableField(exist = false)
    private String levelName;
}
