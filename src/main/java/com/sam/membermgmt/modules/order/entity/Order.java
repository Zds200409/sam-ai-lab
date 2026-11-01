package com.sam.membermgmt.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {
    private String orderNo;
    private Long memberId;
    private String memberName;
    private String memberLevelName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal actualAmount;
    /** 配送费。门店自购为 0；线上订单商品折后金额未满 180 元收 15 元。运费不计入积分基数。 */
    private BigDecimal deliveryFee;
    private Integer pointsEarned;
    private String paymentMethod;
    private Long operatorId;
    private Integer status;
    private String remark;
}
