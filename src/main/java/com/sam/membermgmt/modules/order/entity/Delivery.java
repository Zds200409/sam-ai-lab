package com.sam.membermgmt.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery")
public class Delivery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private String memberName;
    private String memberPhone;
    private String deliveryAddress;
    private Integer status;
    private Long deliveryUserId;
    private LocalDateTime deliveryTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
