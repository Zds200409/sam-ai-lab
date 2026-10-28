package com.sam.membermgmt.modules.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 会员自助提交退换货申请。
 * 对应论文 3.2.2 消费管理需求与图 3.1/3.5 中"会员申请退换货"的描述。
 */
@Data
@Schema(description = "会员退换货申请")
public class MemberReturnDTO {

    @NotNull(message = "订单不能为空")
    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "类型 1=退货 2=换货")
    private Integer type;

    @NotBlank(message = "请填写退换货原因")
    @Schema(description = "退换货原因")
    private String reason;
}
