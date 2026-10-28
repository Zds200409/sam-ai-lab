package com.sam.membermgmt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.membermgmt.common.Result;
import com.sam.membermgmt.common.annotation.RequireRole;
import com.sam.membermgmt.utils.JwtUtil;
import com.sam.membermgmt.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            new ObjectMapper().writeValue(response.getOutputStream(),
                    Result.error(401, "未登录或登录已过期"));
            return false;
        }

        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            new ObjectMapper().writeValue(response.getOutputStream(),
                    Result.error(401, "登录已过期，请重新登录"));
            return false;
        }

        // 按路径命名空间区分 token 类型，防止会员令牌访问后台接口或反之
        String uri = request.getRequestURI();
        boolean memberNamespace = uri.startsWith("/api/member-auth/") || uri.startsWith("/api/portal/");
        String tokenUserType = jwtUtil.getUserTypeFromToken(token);
        if (memberNamespace) {
            if (!"MEMBER".equals(tokenUserType)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                new ObjectMapper().writeValue(response.getOutputStream(),
                        Result.error(401, "请使用会员账号登录后再操作"));
                return false;
            }
        } else {
            if ("MEMBER".equals(tokenUserType)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json;charset=UTF-8");
                new ObjectMapper().writeValue(response.getOutputStream(),
                        Result.error(403, "会员令牌不能访问后台接口"));
                return false;
            }
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        String userType = jwtUtil.getUserTypeFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        List<String> roles = jwtUtil.getRolesFromToken(token);

        // 会员端与后台属于不同 ID 空间，分别写入独立上下文通道
        if ("MEMBER".equals(userType)) {
            Long memberId = jwtUtil.getMemberIdFromToken(token);
            UserContext.setCurrentMemberId(memberId);
        } else {
            UserContext.setCurrentUserId(userId);
        }
        UserContext.setCurrentUsername(username);
        UserContext.setCurrentRoles(roles);

        // 检查 @RequireRole 注解
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }
            if (requireRole != null) {
                String[] requiredRoles = requireRole.value();
                boolean hasPermission = false;
                for (String required : requiredRoles) {
                    if (roles.contains(required)) {
                        hasPermission = true;
                        break;
                    }
                }
                if (!hasPermission) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json;charset=UTF-8");
                    new ObjectMapper().writeValue(response.getOutputStream(),
                            Result.error(403, "权限不足，无法访问该资源"));
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
