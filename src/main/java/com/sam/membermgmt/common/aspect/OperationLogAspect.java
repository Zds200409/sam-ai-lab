package com.sam.membermgmt.common.aspect;

import com.sam.membermgmt.common.annotation.OperationLog;
import com.sam.membermgmt.modules.system.mapper.OperationLogMapper;
import com.sam.membermgmt.utils.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面 — 拦截 @OperationLog 注解的方法，记录操作人、参数、耗时，
 * 并将审计记录持久化到 operation_log 表（落库失败不影响主流程）。
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogAspect(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog opLog) throws Throwable {
        String operation = opLog.value();
        if (operation.isEmpty()) {
            operation = joinPoint.getSignature().getName();
        }

        Long userId = UserContext.getCurrentUserId();
        String username = UserContext.getCurrentUsername();
        String method = joinPoint.getSignature().toShortString();
        String ip = getClientIp();
        String params = safeParams(joinPoint.getArgs());

        log.info("[AUDIT] 操作人: {} (ID:{}), 操作: {}, 方法: {}, IP: {}",
                username, userId, operation, method, ip);

        com.sam.membermgmt.modules.system.entity.OperationLog record = new com.sam.membermgmt.modules.system.entity.OperationLog();
        record.setUserId(userId);
        record.setUsername(username);
        record.setOperation(operation);
        record.setMethod(method);
        record.setIp(ip);
        record.setParams(params);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            record.setDuration(elapsed);
            record.setStatus(1);
            log.info("[AUDIT] 操作成功 — {} (耗时: {}ms)", operation, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            record.setDuration(elapsed);
            record.setStatus(0);
            record.setErrorMsg(truncate(e.getMessage(), 500));
            log.error("[AUDIT] 操作失败 — {} (耗时: {}ms, 异常: {})", operation, elapsed, e.getMessage());
            throw e;
        } finally {
            persist(record);
        }
    }

    private void persist(com.sam.membermgmt.modules.system.entity.OperationLog record) {
        try {
            if (record.getCreateTime() == null) {
                record.setCreateTime(LocalDateTime.now());
            }
            operationLogMapper.insert(record);
        } catch (Exception e) {
            log.warn("[AUDIT] 操作日志落库失败（不影响主流程）: {}", e.getMessage());
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            if (request == null) return null;
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeParams(Object[] args) {
        if (args == null || args.length == 0) return null;
        try {
            return truncate(Arrays.toString(args), 2000);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
