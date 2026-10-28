package com.sam.membermgmt.modules.member.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class MemberLoginVO {

    private String token;

    private Long memberId;

    private String memberNo;

    private String name;

    private String phone;

    private Integer gender;

    private LocalDate birthday;

    private String email;

    private Long levelId;

    private String levelName;

    private Integer totalPoints;

    private Integer accumulatedPoints;

    private BigDecimal totalConsumption;

    private LocalDate expiryDate;

    /** 会员是否已过期（年费到期） */
    private Boolean expired;

    private Integer status;

    private List<String> roles;
}
