package com.sam.membermgmt.modules.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sam.membermgmt.common.enums.OrderStatusEnum;
import com.sam.membermgmt.common.enums.StockTypeEnum;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.service.IPointsService;
import com.sam.membermgmt.modules.order.dto.CartItemDTO;
import com.sam.membermgmt.modules.order.dto.CheckoutRequestDTO;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.entity.Order;
import com.sam.membermgmt.modules.order.entity.OrderItem;
import com.sam.membermgmt.modules.order.entity.Delivery;
import com.sam.membermgmt.modules.order.mapper.DeliveryMapper;
import com.sam.membermgmt.modules.order.mapper.OrderItemMapper;
import com.sam.membermgmt.modules.order.mapper.OrderMapper;
import com.sam.membermgmt.modules.order.service.IOrderService;
import com.sam.membermgmt.modules.product.entity.Product;
import com.sam.membermgmt.modules.product.entity.StockRecord;
import com.sam.membermgmt.modules.product.mapper.ProductMapper;
import com.sam.membermgmt.modules.product.mapper.StockRecordMapper;
import com.sam.membermgmt.utils.OrderNoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberLevelMapper levelMapper;

    @Autowired
    private StockRecordMapper stockRecordMapper;

    @Autowired
    private IPointsService pointsService;

    @Autowired
    private OrderNoGenerator orderNoGenerator;

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO checkout(CheckoutRequestDTO dto, Long operatorId) {
        // 1. 校验会员
        Member member = memberMapper.selectById(dto.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (member.getStatus() != 1) {
            throw new BusinessException("会员状态异常，无法结算");
        }
        MemberLevel level = levelMapper.selectById(member.getCurrentLevelId());
        if (level == null) {
            throw new BusinessException("会员等级不存在");
        }

        // 2. 按商品聚合数量（同一商品在购物车中多次出现需合并校验，避免重复扣减导致超卖）
        Map<Long, Integer> aggregated = new LinkedHashMap<>();
        for (CartItemDTO cartItem : dto.getItems()) {
            aggregated.put(cartItem.getProductId(),
                    aggregated.getOrDefault(cartItem.getProductId(), 0) + cartItem.getQuantity());
        }

        // 3. 校验商品并计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : aggregated.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException("商品[" + productId + "]不存在");
            }
            if (product.getStatus() != 1) {
                throw new BusinessException("商品[" + product.getProductName() + "]已下架");
            }
            if (product.getStockQuantity() < qty) {
                throw new BusinessException("商品[" + product.getProductName() + "]库存不足，当前库存: " + product.getStockQuantity());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getProductName());
            item.setProductCode(product.getProductCode());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(qty);
            item.setSubtotal(subtotal);
            orderItems.add(item);
        }

        // 3. 计算折扣
        BigDecimal discountRate = level.getDiscountRate();
        BigDecimal discountAmount = totalAmount.multiply(
                BigDecimal.ONE.subtract(discountRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = totalAmount.subtract(discountAmount);

        // 4. 计算积分
        int pointsEarned = actualAmount.multiply(level.getPointsRate()).intValue();

        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNoGenerator.generate());
        order.setMemberId(member.getId());
        order.setMemberName(member.getName());
        order.setMemberLevelName(level.getLevelName());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setActualAmount(actualAmount);
        order.setPointsEarned(pointsEarned);
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setOperatorId(operatorId);
        order.setStatus(OrderStatusEnum.COMPLETED.getCode());
        orderMapper.insert(order);

        // 5.1 线上自助下单（operatorId 为 null）时自动生成待配送任务，由配送员接单配送到家
        if (operatorId == null) {
            if (dto.getDeliveryAddress() == null || dto.getDeliveryAddress().trim().isEmpty()) {
                throw new BusinessException("线上配送订单需填写收货地址");
            }
            Delivery delivery = new Delivery();
            delivery.setOrderId(order.getId());
            delivery.setOrderNo(order.getOrderNo());
            delivery.setMemberName(member.getName());
            delivery.setMemberPhone(member.getPhone());
            delivery.setDeliveryAddress(dto.getDeliveryAddress().trim());
            delivery.setStatus(1); // 1=待配送
            deliveryMapper.insert(delivery);
        }

        // 6. 保存订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 7. 聚合扣减库存（原子更新：仅当库存充足时才扣减，防止同商品重复出现或并发导致超卖）
        for (Map.Entry<Long, Integer> entry : aggregated.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();
            int affected = productMapper.decrementStock(productId, qty);
            if (affected == 0) {
                throw new BusinessException("商品库存不足，下单失败，请稍后重试");
            }
            Product product = productMapper.selectById(productId);

            StockRecord stockRecord = new StockRecord();
            stockRecord.setProductId(productId);
            stockRecord.setQuantityChange(-qty);
            stockRecord.setType(StockTypeEnum.SALE_OUT.getCode());
            stockRecord.setStockBefore(product.getStockQuantity() + qty);
            stockRecord.setStockAfter(product.getStockQuantity());
            stockRecord.setOrderId(order.getId());
            stockRecord.setRemark("销售出库");
            stockRecordMapper.insert(stockRecord);
        }

        // 8. 更新会员消费金额
        member.setTotalConsumption(member.getTotalConsumption().add(actualAmount));
        memberMapper.updateById(member);

        // 9. 发放积分
        if (pointsEarned > 0) {
            pointsService.earnPoints(member.getId(), order.getId(), pointsEarned,
                    "消费获得积分，订单号: " + order.getOrderNo());
        }

        // 10. 构造返回
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setMemberId(member.getId());
        vo.setMemberName(member.getName());
        vo.setMemberLevelName(level.getLevelName());
        vo.setTotalAmount(totalAmount);
        vo.setDiscountAmount(discountAmount);
        vo.setActualAmount(actualAmount);
        vo.setPointsEarned(pointsEarned);
        vo.setPaymentMethod(dto.getPaymentMethod());
        vo.setItems(orderItems);
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.COMPLETED.getCode()) {
            throw new BusinessException("仅已完成状态的订单可申请退款");
        }
        order.setStatus(OrderStatusEnum.PENDING_REFUND.getCode());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_REFUND.getCode()) {
            throw new BusinessException("仅待审核状态的订单可审批退款");
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                int stockBefore = product.getStockQuantity();
                int stockAfter = stockBefore + item.getQuantity();
                product.setStockQuantity(stockAfter);
                productMapper.updateById(product);

                StockRecord stockRecord = new StockRecord();
                stockRecord.setProductId(product.getId());
                stockRecord.setQuantityChange(item.getQuantity());
                stockRecord.setType(StockTypeEnum.REFUND_IN.getCode());
                stockRecord.setStockBefore(stockBefore);
                stockRecord.setStockAfter(stockAfter);
                stockRecord.setOrderId(orderId);
                stockRecord.setRemark("退款退货入库");
                stockRecordMapper.insert(stockRecord);
            }
        }

        // 退回积分
        if (order.getPointsEarned() > 0) {
            pointsService.consumePoints(order.getMemberId(), orderId, order.getPointsEarned(),
                    "订单退款退回积分，订单号: " + order.getOrderNo());
        }

        // 退回消费金额
        Member member = memberMapper.selectById(order.getMemberId());
        if (member != null) {
            member.setTotalConsumption(member.getTotalConsumption().subtract(order.getActualAmount()));
            memberMapper.updateById(member);
        }

        order.setStatus(OrderStatusEnum.REFUNDED.getCode());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_REFUND.getCode()) {
            throw new BusinessException("仅待审核状态的订单可拒绝退款");
        }
        order.setStatus(OrderStatusEnum.REFUND_REJECTED.getCode());
        orderMapper.updateById(order);
    }
}
