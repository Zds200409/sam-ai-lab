package com.sam.membermgmt.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("member_level_change_record")
public class MemberLevelChangeRecord implements Serializable {
    private Long id;
    private Long memberId;
    private Long fromLevelId;
    private Long toLevelId;
    private String changeReason;
    private Long operatorId;
    private LocalDateTime createTime;
}
