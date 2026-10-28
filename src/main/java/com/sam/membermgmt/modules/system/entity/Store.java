package com.sam.membermgmt.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sam.membermgmt.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store")
public class Store extends BaseEntity {
    private String storeCode;
    private String storeName;
    private String address;
    private String phone;
    private String managerName;
    private Integer status;
    private String remark;
}
