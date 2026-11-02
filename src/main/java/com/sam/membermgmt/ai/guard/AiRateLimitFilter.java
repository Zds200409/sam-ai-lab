package com.sam.membermgmt.ai.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.membermgmt.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 问答接口的按 IP 限流。
 *
 * <p>AI 助手是公开演示入口，不需要登录，因此任何人都能直接调用，
 * 而每次调用都要真实消耗 DeepSeek 与硅基流动的额度。没有限流就等于把账单
 * 交给陌生人。这里用最朴素的内存滑动窗口挡住这种情况，单实例部署够用。
 *
 * <p>已知局限：计数存在本机内存里，多实例部署时每个实例各算各的，
 * 重启即清零。真正对外运营应换成 Redis 计数或网关层限流，
 * 这里的目标是让公开演示不被刷穿，不是做完整的配额系统。
 */
@Component
public class AiRateLimitFilter extends OncePerRequestFilter {

    private static final String[] GUARDED_PATHS = {
            "/api/ai/chat",
            "/api/ai/chat/stream"
    };

    @Value("${sam.ai.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${sam.ai.rate-limit.per-hour:30}")
    private int perHour;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String path : GUARDED_PATHS) {
            if (uri.equals(path) || uri.startsWith(path + "/")) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String clientKey = clientIp(request);
        long windowStart = System.currentTimeMillis() - 3_600_000L;

        Deque<Long> stamps = hits.computeIfAbsent(clientKey, k -> new ArrayDeque<>());
        boolean allowed;
        long retryAfterSeconds;

        synchronized (stamps) {
            while (!stamps.isEmpty() && stamps.peekFirst() < windowStart) {
                stamps.pollFirst();
            }
            retryAfterSeconds = stamps.isEmpty()
                    ? 0
                    : (stamps.peekFirst() + 3_600_000L - System.currentTimeMillis()) / 1000;
            allowed = stamps.size() < perHour;
            if (allowed) {
                stamps.addLast(System.currentTimeMillis());
            }
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1)));
            objectMapper.writeValue(response.getOutputStream(),
                    Result.error(429, "提问太频繁了，请稍后再试。演示环境每小时限 "
                            + perHour + " 次"));
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 取真实客户端 IP。部署在 nginx 后面时 request.getRemoteAddr() 拿到的
     * 是反向代理地址，所有请求会共用同一个计数桶，所以优先读 X-Forwarded-For。
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
