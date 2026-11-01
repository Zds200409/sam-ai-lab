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
    /** 配送费。门店自购为 0，线上订单商品折后金额未满 180 元为 15 元 */
    private BigDecimal deliveryFee;
    /** 应付总额 = 商品折后金额 + 配送费 */
    private BigDecimal payableAmount;
    private Integer pointsEarned;
    private String paymentMethod;
    private List<OrderItem> items;
    private LocalDateTime createTime;
}
