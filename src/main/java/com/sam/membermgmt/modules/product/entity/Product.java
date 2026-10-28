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

    /** 逻辑删除标记：0 未删除，1 已删除（查询自动过滤） */
    @TableLogic
    private Integer deleted;
}
