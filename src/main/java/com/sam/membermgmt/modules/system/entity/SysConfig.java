package com.sam.membermgmt.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统参数配置（门店信息、积分规则、库存预警阈值、分页条数等）
 * 对应论文 3.2.3「基础配置」与 4.3.3「基础参数维护」。
 */
@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
