package com.sam.membermgmt.modules.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sam.membermgmt.common.exception.BusinessException;
import com.sam.membermgmt.modules.member.dto.LevelUpgradeInfoVO;
import com.sam.membermgmt.modules.member.dto.MemberLoginVO;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberBenefit;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.entity.MemberPointsRecord;
import com.sam.membermgmt.modules.member.mapper.MemberBenefitMapper;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.member.mapper.MemberPointsRecordMapper;
import com.sam.membermgmt.modules.member.service.IMemberAuthService;
import com.sam.membermgmt.modules.member.service.IMemberPortalService;
import com.sam.membermgmt.modules.member.service.IPointsService;
import com.sam.membermgmt.modules.order.dto.CartItemDTO;
import com.sam.membermgmt.modules.order.dto.CheckoutRequestDTO;
import com.sam.membermgmt.modules.order.dto.OrderVO;
import com.sam.membermgmt.modules.order.entity.Order;
import com.sam.membermgmt.modules.order.entity.ReturnRecord;
import com.sam.membermgmt.modules.order.mapper.OrderMapper;
import com.sam.membermgmt.modules.order.service.IOrderService;
import com.sam.membermgmt.modules.product.entity.Product;
import com.sam.membermgmt.modules.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class MemberPortalServiceImpl implements IMemberPortalService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberLevelMapper levelMapper;

    @Autowired
    private MemberBenefitMapper benefitMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MemberPointsRecordMapper pointsRecordMapper;

    @Autowired
    private IPointsService pointsService;

    @Autowired
    private IMemberAuthService memberAuthService;

    @Autowired
    private com.sam.membermgmt.modules.order.mapper.ReturnRecordMapper returnRecordMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private IOrderService orderService;

    @Override
    public MemberLoginVO summary(Long memberId) {
        return memberAuthService.getProfile(memberId);
    }

    @Override
    public IPage<Order> pageOrders(Long memberId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMemberId, memberId)
                .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public IPage<MemberPointsRecord> pagePointsRecords(Long memberId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<MemberPointsRecord> wrapper = new LambdaQueryWrapper<MemberPointsRecord>()
                .eq(MemberPointsRecord::getMemberId, memberId)
                .orderByDesc(MemberPointsRecord::getCreateTime);
        return pointsRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<MemberBenefit> listBenefits(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return benefitMapper.selectList(new LambdaQueryWrapper<MemberBenefit>()
                .eq(MemberBenefit::getLevelId, member.getCurrentLevelId())
                .eq(MemberBenefit::getStatus, 1)
                .orderByAsc(MemberBenefit::getPointsCost)
                .orderByAsc(MemberBenefit::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeem(Long memberId, Long benefitId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        MemberBenefit benefit = benefitMapper.selectById(benefitId);
        if (benefit == null || (benefit.getStatus() != null && benefit.getStatus() == 0)) {
            throw new BusinessException("权益不存在或已下架");
        }
        if (!Objects.equals(benefit.getLevelId(), member.getCurrentLevelId())) {
            throw new BusinessException("该权益不属于您当前等级");
        }
        if (benefit.getPointsCost() == null || benefit.getPointsCost() <= 0) {
            throw new BusinessException("该权益不可用积分兑换");
        }
        pointsService.consumePoints(memberId, null, benefit.getPointsCost(), "兑换权益：" + benefit.getBenefitName());
    }

    @Override
    public IPage<Product> listProducts(Long categoryId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public OrderVO placeOrder(Long memberId, List<CartItemDTO> items, String paymentMethod, String deliveryAddress) {
        CheckoutRequestDTO dto = new CheckoutRequestDTO();
        dto.setMemberId(memberId);
        dto.setItems(items);
        dto.setPaymentMethod(StringUtils.hasText(paymentMethod) ? paymentMethod : "在线支付");
        dto.setDeliveryAddress(deliveryAddress);
        // 会员自助下单为线上配送场景，无收银员操作，operatorId 传 null
        return orderService.checkout(dto, null);
    }

    @Override
    public LevelUpgradeInfoVO upgradeInfo(Long memberId) {
        LevelUpgradeInfoVO vo = new LevelUpgradeInfoVO();
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return vo;
        }
        MemberLevel current = levelMapper.selectById(member.getCurrentLevelId());
        if (current == null) {
            return vo;
        }
        vo.setCurrentLevelId(current.getId());
        vo.setCurrentLevelName(current.getLevelName());

        MemberLevel next = levelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>()
                        .gt(MemberLevel::getSortOrder, current.getSortOrder())
                        .orderByAsc(MemberLevel::getSortOrder)
                        .last("LIMIT 1"));
        if (next != null) {
            vo.setUpgradable(true);
            vo.setNextLevelId(next.getId());
            vo.setNextLevelName(next.getLevelName());
            BigDecimal diff = next.getAnnualFee().subtract(current.getAnnualFee());
            vo.setDiffAmount(diff.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : diff);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyReturn(Long memberId, Long orderId, Integer type, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getMemberId(), memberId)) {
            throw new BusinessException("只能对本人订单发起退换货申请");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException("仅已完成的订单可申请退换货");
        }
        Long pending = returnRecordMapper.selectCount(new LambdaQueryWrapper<ReturnRecord>()
                .eq(ReturnRecord::getOrderId, orderId)
                .eq(ReturnRecord::getStatus, 1));
        if (pending != null && pending > 0) {
            throw new BusinessException("该订单已有待审核的退换货申请，请勿重复提交");
        }
        Member member = memberMapper.selectById(memberId);
        ReturnRecord record = new ReturnRecord();
        record.setOrderId(orderId);
        record.setOrderNo(order.getOrderNo());
        record.setMemberId(memberId);
        record.setMemberName(member == null ? null : member.getName());
        record.setType(type == null ? 1 : type);
        record.setReason(reason);
        record.setAmount(order.getActualAmount());
        record.setStatus(1);
        returnRecordMapper.insert(record);
    }

    @Override
    public IPage<ReturnRecord> pageReturns(Long memberId, Integer pageNum, Integer pageSize) {
        return returnRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ReturnRecord>()
                        .eq(ReturnRecord::getMemberId, memberId)
                        .orderByDesc(ReturnRecord::getCreateTime));
    }
}
