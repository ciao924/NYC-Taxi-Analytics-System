package com.taxi.analytics.common.config;

import com.taxi.analytics.modules.ai.client.SseStreamHttpMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * RestTemplate 全局配置
 * 
 * 核心功能：
 * 1. 配置支持SSE（Server-Sent Events）流式响应
 * 2. 设置合理的超时时间
 * 3. 统一管理HTTP客户端配置
 * 
 * 修复问题：
 * - RestTemplate默认不支持text/event-stream内容类型返回InputStream
 * - 错误信息：Could not extract response: no suitable HttpMessageConverter found 
 *   for response type [class java.io.InputStream] and content type [text/event-stream;charset=utf-8]
 */
@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    /**
     * 创建支持SSE流式响应的RestTemplate Bean
     * 
     * @param builder RestTemplateBuilder
     * @return 配置好的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                // 设置连接超时（Spring Boot 3.1.x 使用 setTimeout 方法）
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                // 设置读取超时（SSE流式响应需要较长的超时时间）
                .setReadTimeout(java.time.Duration.ofSeconds(60))
                .build();

        // 注册自定义的SSE流式响应转换器（添加到最前面，优先级最高）
        registerSseConverter(restTemplate);

        log.info("RestTemplate 配置完成，已注册SSE流式响应转换器");
        return restTemplate;
    }

    /**
     * 注册SSE流式响应转换器
     * 
     * @param restTemplate RestTemplate实例
     */
    private void registerSseConverter(RestTemplate restTemplate) {
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        
        // 检查是否已存在SSE转换器，避免重复注册
        boolean exists = converters.stream()
                .anyMatch(c -> c instanceof SseStreamHttpMessageConverter);
        
        if (!exists) {
            // 将SSE转换器添加到最前面，确保优先使用
            converters.add(0, new SseStreamHttpMessageConverter());
            log.info("已注册 SseStreamHttpMessageConverter 到 RestTemplate");
        } else {
            log.info("SseStreamHttpMessageConverter 已存在，无需重复注册");
        }
    }
}