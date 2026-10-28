package com.sam.membermgmt.modules.member.dto;

import com.sam.membermgmt.modules.order.dto.CartItemDTO;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 会员自助下单请求。会员身份由登录态决定，故无需提交 memberId。
 * 会员端为线上配送场景，需填写收货地址，由配送员配送到家；
 * 支付方式由前端传入（如在线支付），区别于线下到店收银结算。
 */
@Data
public class MemberOrderDTO {

    @NotEmpty(message = "购物清单不能为空")
    private List<CartItemDTO> items;

    private String paymentMethod;

    /**
     * 线上配送收货地址。会员网上下单后系统据此自动生成配送任务。
     */
    private String deliveryAddress;
}
