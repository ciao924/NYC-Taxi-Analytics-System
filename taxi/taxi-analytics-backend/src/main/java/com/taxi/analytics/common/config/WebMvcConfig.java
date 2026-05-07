package com.taxi.analytics.common.config;

import com.taxi.analytics.common.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册限流拦截器，对所有接口进行限流
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/swagger-ui.html", "/api/v3/api-docs/**");
    }
}