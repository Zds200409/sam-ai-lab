package com.sam.membermgmt.modules.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.order.dto.CheckoutRequestDTO;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.entity.Order;
import com.sam.membermgmt.modules.order.entity.OrderItem;
import com.sam.membermgmt.modules.order.mapper.OrderItemMapper;
import com.sam.membermgmt.modules.order.mapper.OrderMapper;
import com.sam.membermgmt.modules.order.service.IOrderService;
import com.sam.membermgmt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"})
public class OrderController {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @OperationLog("POS收银结算")
    @Operation(summary = "POS收银结算")
    @PostMapping("/checkout")
    public Result<OrderVO> checkout(@Valid @RequestBody CheckoutRequestDTO dto) {
        OrderVO vo = orderService.checkout(dto, UserContext.getCurrentUserId());
        return Result.success("结算成功", vo);
    }

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    public Result<PageResult<Order>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String memberName,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .like(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                .like(StringUtils.hasText(memberName), Order::getMemberName, memberName)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);
        IPage<Order> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));

        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setMemberId(order.getMemberId());
        vo.setMemberName(order.getMemberName());
        vo.setMemberLevelName(order.getMemberLevelName());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setActualAmount(order.getActualAmount());
        vo.setPointsEarned(order.getPointsEarned());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setItems(items);
        vo.setCreateTime(order.getCreateTime());
        return Result.success(vo);
    }

    @OperationLog("申请退款")
    @Operation(summary = "申请退款")
    @PostMapping("/{id}/refund")
    public Result<?> refund(@PathVariable Long id) {
        orderService.applyRefund(id);
        return Result.success("退款申请已提交");
    }

    @OperationLog("审批退款通过")
    @Operation(summary = "审批退款通过")
    @PostMapping("/{id}/approve-refund")
    public Result<?> approveRefund(@PathVariable Long id) {
        orderService.approveRefund(id);
        return Result.success("退款已审核通过");
    }

    @OperationLog("审批退款拒绝")
    @Operation(summary = "审批退款拒绝")
    @PostMapping("/{id}/reject-refund")
    public Result<?> rejectRefund(@PathVariable Long id) {
        orderService.rejectRefund(id);
        return Result.success("退款申请已拒绝");
    }

    @Operation(summary = "按订单号查询")
    @GetMapping("/search")
    public Result<OrderVO> search(@RequestParam String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return detail(order.getId());
    }
}
