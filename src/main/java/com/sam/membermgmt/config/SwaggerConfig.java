package com.sam.membermgmt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("山姆会员消费管理系统 API")
                        .version("1.0.0")
                        .description("基于Spring Boot的会员消费全流程管理平台")
                        .contact(new Contact().name("赵佳源")));
    }
}
