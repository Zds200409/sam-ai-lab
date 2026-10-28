package com.sam.membermgmt.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 — 记录用户操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    String value() default "";
}
