package com.sam.membermgmt.modules.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sam.membermgmt.modules.order.dto.CheckoutRequestDTO;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.entity.Order;

public interface IOrderService extends IService<Order> {
    OrderVO checkout(CheckoutRequestDTO dto, Long operatorId);
    /** 申请退款：COMPLETED → PENDING_REFUND */
    void applyRefund(Long orderId);
    /** 审批通过：PENDING_REFUND → REFUNDED（恢复库存+退回积分） */
    void approveRefund(Long orderId);
    /** 审批拒绝：PENDING_REFUND → REFUND_REJECTED */
    void rejectRefund(Long orderId);
}
