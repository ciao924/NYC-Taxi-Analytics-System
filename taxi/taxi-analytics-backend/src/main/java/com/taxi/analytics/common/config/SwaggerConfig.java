package com.taxi.analytics.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("出租车数据分析系统 API接口文档")
                        .version("v2.0")
                        .description("基于Spring Boot 3.2.x构建的出租车数据分析后端服务接口")
                        .contact(new Contact().name("架构组")));
    }
}