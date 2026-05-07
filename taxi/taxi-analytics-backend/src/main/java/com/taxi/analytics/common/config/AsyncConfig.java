package com.taxi.analytics.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // 异步支持配置已通过@EnableAsync开启，具体线程池在ThreadPoolConfig中配置
}