package com.sam.membermgmt.modules.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_category")
public class ProductCategory extends BaseEntity {
    private Long parentId;
    private String categoryName;
    private Integer sortOrder;
    private String icon;

    @TableField(exist = false)
    private List<ProductCategory> children;
}
