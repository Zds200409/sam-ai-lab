package com.sam.membermgmt.modules.order.dto;

import com.sam.membermgmt.modules.order.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long orderId;
    private String orderNo;
    private Long memberId;
    private String memberName;
    private String memberLevelName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal actualAmount;
    private Integer pointsEarned;
    private String paymentMethod;
    private List<OrderItem> items;
    private LocalDateTime createTime;
}
