package com.sam.membermgmt.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("return_record")
public class ReturnRecord extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private Long memberId;
    private String memberName;
    private Integer type;
    private String reason;
    private BigDecimal amount;
    private Integer status;
    private Long handlerId;
    private LocalDateTime handleTime;
}
