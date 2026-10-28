package com.sam.membermgmt.modules.member.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.modules.member.dto.LevelUpgradeInfoVO;
import com.sam.membermgmt.modules.member.dto.MemberLoginVO;
import com.sam.membermgmt.modules.member.dto.MemberOrderDTO;
import com.sam.membermgmt.modules.member.dto.MemberReturnDTO;
import com.sam.membermgmt.modules.member.entity.MemberBenefit;
import com.sam.membermgmt.modules.member.entity.MemberPointsRecord;
import com.sam.membermgmt.modules.member.service.IMemberPortalService;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.entity.Order;
import com.sam.membermgmt.modules.order.entity.ReturnRecord;
import com.sam.membermgmt.modules.product.entity.Product;
import com.sam.membermgmt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "会员门户")
@RestController
@RequestMapping("/api/portal")
public class MemberPortalController {

    @Autowired
    private IMemberPortalService portalService;

    @Operation(summary = "我的首页汇总")
    @GetMapping("/summary")
    public Result<MemberLoginVO> summary() {
        return Result.success(portalService.summary(UserContext.getCurrentMemberId()));
    }

    @Operation(summary = "升级预览（当前等级/目标等级/补差价）")
    @GetMapping("/upgrade-info")
    public Result<LevelUpgradeInfoVO> upgradeInfo() {
        return Result.success(portalService.upgradeInfo(UserContext.getCurrentMemberId()));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/orders")
    public Result<IPage<Order>> orders(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(portalService.pageOrders(UserContext.getCurrentMemberId(), pageNum, pageSize));
    }

    @Operation(summary = "我的积分流水")
    @GetMapping("/points-records")
    public Result<IPage<MemberPointsRecord>> pointsRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(portalService.pagePointsRecords(UserContext.getCurrentMemberId(), pageNum, pageSize));
    }

    @Operation(summary = "我所在等级的可兑换权益")
    @GetMapping("/benefits")
    public Result<List<MemberBenefit>> benefits() {
        return Result.success(portalService.listBenefits(UserContext.getCurrentMemberId()));
    }

    @Operation(summary = "积分兑换权益")
    @PostMapping("/redeem/{benefitId}")
    public Result<Void> redeem(@PathVariable Long benefitId) {
        portalService.redeem(UserContext.getCurrentMemberId(), benefitId);
        return Result.success("兑换成功", null);
    }

    @Operation(summary = "商品列表（会员选购）")
    @GetMapping("/products")
    public Result<IPage<Product>> products(@RequestParam(required = false) Long categoryId,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(portalService.listProducts(categoryId, pageNum, pageSize));
    }

    @Operation(summary = "会员自助下单")
    @PostMapping("/order")
    public Result<OrderVO> placeOrder(@Valid @RequestBody MemberOrderDTO dto) {
        OrderVO vo = portalService.placeOrder(UserContext.getCurrentMemberId(), dto.getItems(), dto.getPaymentMethod(), dto.getDeliveryAddress());
        return Result.success("下单成功", vo);
    }

    @Operation(summary = "申请退换货")
    @PostMapping("/return")
    public Result<Void> applyReturn(@Valid @RequestBody MemberReturnDTO dto) {
        portalService.applyReturn(UserContext.getCurrentMemberId(), dto.getOrderId(), dto.getType(), dto.getReason());
        return Result.success("退换货申请已提交", null);
    }

    @Operation(summary = "我的退换货记录")
    @GetMapping("/returns")
    public Result<IPage<ReturnRecord>> returns(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(portalService.pageReturns(UserContext.getCurrentMemberId(), pageNum, pageSize));
    }
}
