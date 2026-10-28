package com.sam.membermgmt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StockTypeEnum {
    STOCK_IN(1, "入库"),
    SALE_OUT(2, "销售出库"),
    REFUND_IN(3, "退货入库"),
    CHECK(4, "盘点调整");

    private final int code;
    private final String desc;
}
