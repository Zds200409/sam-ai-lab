package com.sam.membermgmt.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("member_points_record")
public class MemberPointsRecord implements Serializable {
    private Long id;
    private Long memberId;
    private Long orderId;
    private Integer points;
    private Integer type;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String description;
    private LocalDateTime createTime;
}
