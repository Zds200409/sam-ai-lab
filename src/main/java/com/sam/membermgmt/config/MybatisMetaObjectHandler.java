package com.sam.membermgmt.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充。
 *
 * <p>背景：{@code BaseEntity} 对 createTime / updateTime 标注了
 * {@code @TableField(fill = FieldFill.INSERT)} 与 {@code INSERT_UPDATE}，
 * 但此前项目中没有提供 {@link MetaObjectHandler} 实现。
 * 结果是这两列会以 NULL 参与 INSERT，覆盖数据库的
 * {@code DEFAULT CURRENT_TIMESTAMP}，导致所有经界面新建的记录
 * （会员、商品、订单、公告、退换货申请等）创建时间为空。
 *
 * <p>本类补齐该处理器：新增时填充 createTime 与 updateTime，更新时刷新 updateTime。
 * 已显式赋值（如 OperationLogAspect 手工设置的日志时间）不会被覆盖。
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
