package com.sam.membermgmt.modules.member.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 会员自助维护个人信息。仅允许修改姓名、性别、生日、邮箱，
 * 手机号、等级、积分、余额等敏感字段由后台统一管理。
 */
@Data
public class MemberProfileDTO {

    private String name;

    private Integer gender;

    private LocalDate birthday;

    private String email;
}
