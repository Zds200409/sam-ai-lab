package com.sam.membermgmt.modules.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.PageResult;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.order.entity.Delivery;
import com.sam.membermgmt.modules.order.entity.Order;
import com.sam.membermgmt.modules.order.mapper.DeliveryMapper;
import com.sam.membermgmt.modules.order.mapper.OrderMapper;
import com.sam.membermgmt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "配送管理")
@RestController
@RequestMapping("/api/delivery")
@RequireRole({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_DELIVERY"})
public class DeliveryController {

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Operation(summary = "配送订单分页")
    @GetMapping("/page")
    public Result<PageResult<Delivery>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Delivery> wrapper = new LambdaQueryWrapper<Delivery>()
                .eq(status != null, Delivery::getStatus, status)
                .orderByDesc(Delivery::getCreateTime);
        IPage<Delivery> page = deliveryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "生成配送单（收银结算后调用）")
    @PostMapping("/create")
    public Result<?> create(@RequestParam Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        // 订单与配送单为 1:1（delivery.order_id 已建唯一键），此处先做友好提示，避免直接抛数据库唯一键冲突
        Long exists = deliveryMapper.selectCount(
                new LambdaQueryWrapper<Delivery>().eq(Delivery::getOrderId, orderId));
        if (exists != null && exists > 0) {
            throw new BusinessException("该订单已生成配送单，请勿重复生成");
        }
        Delivery d = new Delivery();
        d.setOrderId(order.getId());
        d.setOrderNo(order.getOrderNo());
        d.setMemberName(order.getMemberName());
        d.setDeliveryAddress("门店配送");
        d.setStatus(1);
        deliveryMapper.insert(d);
        return Result.success("配送单已生成");
    }

    @OperationLog("接单配送")
    @Operation(summary = "接单（待配送→配送中）")
    @PutMapping("/{id}/pickup")
    public Result<?> pickup(@PathVariable Long id) {
        Delivery d = deliveryMapper.selectById(id);
        if (d == null || d.getStatus() != 1) throw new BusinessException("该配送单不可接单");
        d.setStatus(2);
        d.setDeliveryUserId(UserContext.getCurrentUserId());
        deliveryMapper.updateById(d);
        return Result.success("已接单");
    }

    @OperationLog("配送完成")
    @Operation(summary = "送达（配送中→已送达）")
    @PutMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id) {
        Delivery d = deliveryMapper.selectById(id);
        if (d == null || d.getStatus() != 2) throw new BusinessException("该配送单不可完成");
        d.setStatus(3);
        d.setDeliveryTime(LocalDateTime.now());
        deliveryMapper.updateById(d);
        return Result.success("配送已完成");
    }
}
