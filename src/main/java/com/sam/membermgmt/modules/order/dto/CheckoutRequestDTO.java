package com.sam.membermgmt.modules.order.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @NotEmpty(message = "购物车不能为空")
    private List<CartItemDTO> items;

    @NotNull(message = "支付方式不能为空")
    private String paymentMethod;

    /**
     * 线上自助下单（会员端）的收货地址；线下到店由收银员录入时无需填写。
     */
    private String deliveryAddress;
}
