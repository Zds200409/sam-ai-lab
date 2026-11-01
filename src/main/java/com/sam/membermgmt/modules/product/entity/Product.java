package com.sam.membermgmt.modules.product.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {
    private Long categoryId;
    private String productName;
    private String productCode;
    private String barcode;
    private String unit;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Integer minStockAlert;
    private String imageUrl;
    private String description;
    private Integer status;

    /**
     * 是否不参与会员折扣：0 参与，1 不参与。
     * 礼品卡、烟酒、特价商品打折会破坏价格体系，单独标记比按分类硬编码更灵活。
     */
    private Integer discountExcluded;

    /** 逻辑删除标记：0 未删除，1 已删除（查询自动过滤） */
    @TableLogic
    private Integer deleted;
}
