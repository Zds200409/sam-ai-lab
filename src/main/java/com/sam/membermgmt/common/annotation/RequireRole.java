package com.sam.membermgmt.common.annotation;

import java.lang.annotation.*;

/**
 * 角色权限注解 — 标记接口需要的角色
 * 可用于类级别（默认）或方法级别（覆盖类级别）
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value();
}
