package com.sam.membermgmt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/captcha",
                        "/api/auth/refresh",
                        "/api/member-auth/register",
                        "/api/member-auth/login",
                        "/api/member-auth/reset-password",
                        // AI 问答对公开演示开放，免登录，但由 AiRateLimitFilter 按 IP 限流。
                        // 只放行两个问答接口，/api/ai/admin/** 仍然要求管理员登录。
                        "/api/ai/chat",
                        "/api/ai/chat/stream",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
