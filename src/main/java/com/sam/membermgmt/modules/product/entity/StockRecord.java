package com.sam.membermgmt.modules.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("stock_record")
public class StockRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Integer quantityChange;
    private Integer type;
    private Integer stockBefore;
    private Integer stockAfter;
    private Long orderId;
    private String remark;
    private LocalDateTime createTime;
}
