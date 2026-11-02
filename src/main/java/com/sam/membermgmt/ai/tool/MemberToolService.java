package com.sam.membermgmt.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sam.membermgmt.common.enums.MemberStatusEnum;
import com.sam.membermgmt.common.enums.OrderStatusEnum;
import com.sam.membermgmt.modules.member.entity.Member;
import com.sam.membermgmt.modules.member.entity.MemberLevel;
import com.sam.membermgmt.modules.member.mapper.MemberLevelMapper;
import com.sam.membermgmt.modules.member.mapper.MemberMapper;
import com.sam.membermgmt.modules.order.entity.Order;
import com.sam.membermgmt.modules.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 会员数据查询工具，供大模型在对话中调用。
 *
 * 为什么需要这一层：知识库能回答「卓越会员几折」这类通用政策，
 * 但回答不了「我的积分还有多少」——那需要看具体会员的数据。
 * 没有工具时模型只能说「我看不到个人数据」，用户体验是断的。
 *
 * 安全边界（重要）：这些工具只按手机号查询，没有二次验证。
 * 演示环境可以接受，因为数据是虚构的；生产环境必须先做短信验证码校验，
 * 否则任何人报出他人手机号就能拿到等级、积分和订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberToolService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ORDER_LIMIT = 5;

    private final MemberMapper memberMapper;
    private final MemberLevelMapper levelMapper;
    private final OrderMapper orderMapper;

    @Tool(name = "queryMemberLevel",
            description = "根据会员手机号查询会员等级、账户状态和有效期。"
                    + "用户询问自己是什么等级、会员卡什么时候到期、卡还能不能用、累计消费多少时调用。")
    public String queryMemberLevel(
            @ToolParam(description = "会员注册手机号，11 位数字") String phone) {

        Member member = findByPhone(phone);
        if (member == null) {
            log.info("[TOOL] 查询手机号 {} -> 未查询到会员", phone);
            return "未查询到手机号 " + phone + " 对应的会员，请确认号码是否正确。";
        }

        MemberLevel level = levelMapper.selectById(member.getCurrentLevelId());
        String levelName = level != null ? level.getLevelName() : "未知等级";
        String levelRule = level != null
                ? "积分倍率 " + level.getPointsRate() + "，消费享 " + toDiscountLabel(level.getDiscountRate())
                : "";
        String expiry = member.getExpiryDate() != null ? member.getExpiryDate().format(DATE_FMT) : "未记录";
        String status = MemberStatusEnum.getDesc(member.getStatus());

        log.info("[TOOL] queryMemberLevel phone={} -> {} / {}", phone, levelName, status);

        return String.format(
                "会员姓名 %s，等级 %s（%s），账户状态 %s，有效期至 %s，累计消费 %s 元。",
                member.getName(), levelName, levelRule, status, expiry,
                member.getTotalConsumption() == null ? "0" : member.getTotalConsumption().toPlainString());
    }

    @Tool(name = "queryPoints",
            description = "根据会员手机号查询积分余额和累计获得的积分。"
                    + "用户询问自己还有多少积分、积分够不够兑换某个权益时调用。")
    public String queryPoints(
            @ToolParam(description = "会员注册手机号，11 位数字") String phone) {

        Member member = findByPhone(phone);
        if (member == null) {
            log.info("[TOOL] 查询手机号 {} -> 未查询到会员", phone);
            return "未查询到手机号 " + phone + " 对应的会员，请确认号码是否正确。";
        }

        log.info("[TOOL] queryPoints phone={} -> {}", phone, member.getTotalPoints());

        return String.format(
                "会员 %s 当前可用积分 %d 分，历史累计获得 %d 分。",
                member.getName(),
                member.getTotalPoints() == null ? 0 : member.getTotalPoints(),
                member.getAccumulatedPoints() == null ? 0 : member.getAccumulatedPoints());
    }

    @Tool(name = "queryRecentOrders",
            description = "根据会员手机号查询最近的订单，返回订单号、实付金额、状态和下单时间。"
                    + "用户询问自己最近买了什么、订单状态、有没有退款时调用。")
    public String queryRecentOrders(
            @ToolParam(description = "会员注册手机号，11 位数字") String phone,
            @ToolParam(required = false, description = "返回几笔订单，1 到 5，默认 3") Integer limit) {

        Member member = findByPhone(phone);
        if (member == null) {
            log.info("[TOOL] 查询手机号 {} -> 未查询到会员", phone);
            return "未查询到手机号 " + phone + " 对应的会员，请确认号码是否正确。";
        }

        int size = limit == null ? 3 : Math.max(1, Math.min(limit, MAX_ORDER_LIMIT));
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getMemberId, member.getId())
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT " + size));

        log.info("[TOOL] queryRecentOrders phone={} limit={} -> {} 笔", phone, size, orders.size());

        if (orders.isEmpty()) {
            return "会员 " + member.getName() + " 名下没有查询到订单记录。";
        }

        StringBuilder sb = new StringBuilder("会员 ").append(member.getName())
                .append(" 最近 ").append(orders.size()).append(" 笔订单：\n");
        int idx = 1;
        for (Order o : orders) {
            sb.append(idx++).append(". 单号 ").append(o.getOrderNo())
                    .append("，实付 ").append(o.getActualAmount() == null ? "0" : o.getActualAmount().toPlainString())
                    .append(" 元");
            if (o.getDeliveryFee() != null && o.getDeliveryFee().signum() > 0) {
                sb.append("（含配送费 ").append(o.getDeliveryFee().toPlainString()).append(" 元）");
            }
            sb.append("，状态 ").append(orderStatusDesc(o.getStatus()))
                    .append("，下单时间 ")
                    .append(o.getCreateTime() == null ? "未记录" : o.getCreateTime().format(DATETIME_FMT))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private Member findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, phone.trim())
                .last("LIMIT 1"));
    }

    /** 0.88 说成 88 折，比直接说「折扣系数 0.88」更像客服话术 */
    private String toDiscountLabel(java.math.BigDecimal discountRate) {
        if (discountRate == null) {
            return "无折扣";
        }
        return discountRate.multiply(new java.math.BigDecimal("100"))
                .stripTrailingZeros().toPlainString() + " 折";
    }

    /** OrderStatusEnum 只有实例方法 getDesc()，这里按 code 反查 */
    private String orderStatusDesc(Integer code) {
        if (code == null) {
            return "未知";
        }
        for (OrderStatusEnum e : OrderStatusEnum.values()) {
            if (e.getCode() == code) {
                return e.getDesc();
            }
        }
        return "未知";
    }
}
