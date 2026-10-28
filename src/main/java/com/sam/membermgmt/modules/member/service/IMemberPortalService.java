package com.sam.membermgmt.modules.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sam.membermgmt.modules.member.dto.LevelUpgradeInfoVO;
import com.sam.membermgmt.modules.member.dto.MemberLoginVO;
import com.sam.membermgmt.modules.member.entity.MemberBenefit;
import com.sam.membermgmt.modules.member.entity.MemberPointsRecord;
import com.sam.membermgmt.modules.order.dto.CartItemDTO;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.entity.Order;
import com.sam.membermgmt.modules.order.entity.ReturnRecord;
import com.sam.membermgmt.modules.product.entity.Product;

import java.util.List;

public interface IMemberPortalService {

    MemberLoginVO summary(Long memberId);

    IPage<Order> pageOrders(Long memberId, Integer pageNum, Integer pageSize);

    IPage<MemberPointsRecord> pagePointsRecords(Long memberId, Integer pageNum, Integer pageSize);

    List<MemberBenefit> listBenefits(Long memberId);

    void redeem(Long memberId, Long benefitId);

    IPage<Product> listProducts(Long categoryId, Integer pageNum, Integer pageSize);

    OrderVO placeOrder(Long memberId, List<CartItemDTO> items, String paymentMethod, String deliveryAddress);

    /** 升级预览信息（当前等级、目标等级、补差价） */
    LevelUpgradeInfoVO upgradeInfo(Long memberId);

    /** 会员自助提交退换货申请（仅限本人已完成订单，且同一订单不能有重复待审申请） */
    void applyReturn(Long memberId, Long orderId, Integer type, String reason);

    /** 我的退换货记录 */
    IPage<ReturnRecord> pageReturns(Long memberId, Integer pageNum, Integer pageSize);
}
