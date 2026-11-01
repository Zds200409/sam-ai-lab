package com.sam.membermgmt.modules.order.service.impl;

import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.order.dto.CartItemDTO;
import com.sam.membermgmt.modules.order.dto.CheckoutRequestDTO;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.service.IOrderService;
import com.sam.membermgmt.modules.product.entity.Product;
import com.sam.membermgmt.modules.product.mapper.ProductMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 定价规则集成测试：会员折扣的排除范围、配送费收取规则。
 *
 * 走真实 Spring 上下文与数据库，整类带 @Transactional，测试结束全部回滚，
 * 不会留下订单、不会扣减库存、不会发放积分。
 *
 * 依赖种子数据：卓越会员（等级 3）、一个参与折扣的商品、一个礼品卡类商品。
 * 种子数据缺失时自动跳过，避免在空库环境下误报失败。
 */
@SpringBootTest
@Transactional
class OrderPricingRulesTest {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ProductMapper productMapper;

    private Long memberId;
    private Long normalProductId;
    private Long excludedProductId;

    @BeforeEach
    void findSeedData() {
        Member member = memberMapper.selectList(null).stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .filter(m -> m.getCurrentLevelId() != null && m.getCurrentLevelId() == 3L)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(member != null, "缺少正常状态的卓越会员种子数据");
        this.memberId = member.getId();

        Product normal = productMapper.selectList(null).stream()
                .filter(p -> p.getDiscountExcluded() != null && p.getDiscountExcluded() == 0)
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 5)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(normal != null, "缺少参与折扣的在售商品");

        Product excluded = productMapper.selectList(null).stream()
                .filter(p -> p.getDiscountExcluded() != null && p.getDiscountExcluded() == 1)
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 5)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(excluded != null, "缺少不参与折扣的商品");

        this.normalProductId = normal.getId();
        this.excludedProductId = excluded.getId();
    }

    private CheckoutRequestDTO build(Long... productIds) {
        CheckoutRequestDTO dto = new CheckoutRequestDTO();
        dto.setMemberId(memberId);
        dto.setPaymentMethod("在线支付");
        dto.setDeliveryAddress("湖北省恩施市金子坝路 1 号");
        dto.setItems(Arrays.stream(productIds).map(id -> {
            CartItemDTO c = new CartItemDTO();
            c.setProductId(id);
            c.setQuantity(1);
            return c;
        }).toList());
        return dto;
    }

    /** 不参与折扣的商品只计价不打折，但仍计入商品总额 */
    @Test
    void excludedProductIsPricedAtFullAmountWhileNormalProductGetsDiscount() {
        BigDecimal normalPrice = productMapper.selectById(normalProductId).getPrice();
        BigDecimal excludedPrice = productMapper.selectById(excludedProductId).getPrice();
        BigDecimal total = normalPrice.add(excludedPrice);
        BigDecimal expectedDiscount = normalPrice.multiply(new BigDecimal("0.12"))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        OrderVO vo = orderService.checkout(build(normalProductId, excludedProductId), null);

        assertEquals(0, total.compareTo(vo.getTotalAmount()), "商品总额含不参与折扣的商品原价");
        assertEquals(0, expectedDiscount.compareTo(vo.getDiscountAmount()), "折扣只按参与折扣的商品计算");
        assertEquals(0, total.subtract(expectedDiscount).compareTo(vo.getActualAmount()), "折后金额");
    }

    /** 线上订单未达免运费门槛收 15 元，且运费不计入积分基数 */
    @Test
    void onlineOrderBelowThresholdIsChargedDeliveryFee() {
        BigDecimal normalPrice = productMapper.selectById(normalProductId).getPrice();
        BigDecimal actual = normalPrice.subtract(normalPrice.multiply(new BigDecimal("0.12"))
                .setScale(2, java.math.RoundingMode.HALF_UP));

        OrderVO vo = orderService.checkout(build(normalProductId), null);

        if (actual.compareTo(new BigDecimal("180")) < 0) {
            assertEquals(0, new BigDecimal("15.00").compareTo(vo.getDeliveryFee()), "未满 180 收 15 元");
            assertEquals(0, actual.add(new BigDecimal("15.00")).compareTo(vo.getPayableAmount()), "应付含运费");
        } else {
            assertEquals(0, BigDecimal.ZERO.compareTo(vo.getDeliveryFee()), "满 180 免运费");
        }
        int expectedPoints = actual.multiply(new BigDecimal("1.5")).intValue();
        assertEquals(expectedPoints, vo.getPointsEarned(), "积分数为商品折后金额乘倍率，不含运费");
    }

    /** 门店订单不涉及配送，始终不收运费 */
    @Test
    void inStoreOrderIsNeverChargedDeliveryFee() {
        OrderVO vo = orderService.checkout(build(normalProductId), 1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getDeliveryFee()), "门店订单不收运费");
        assertEquals(0, vo.getActualAmount().compareTo(vo.getPayableAmount()), "应付等于商品折后金额");
    }

    /** 只买不参与折扣的商品时，折扣金额为零 */
    @Test
    void excludedProductAloneGetsNoDiscount() {
        BigDecimal excludedPrice = productMapper.selectById(excludedProductId).getPrice();

        OrderVO vo = orderService.checkout(build(excludedProductId), null);

        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getDiscountAmount()), "不参与折扣的商品不打折");
        assertEquals(0, excludedPrice.compareTo(vo.getActualAmount()), "按原价结算");
    }
}
