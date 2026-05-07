package com.taxi.analytics.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.taxi.analytics.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * API限流拦截器
 * 根据接口类型配置不同的QPS限制
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // 看板接口限流：10 QPS
    private static final RateLimiter DASHBOARD_LIMITER = RateLimiter.create(10.0);
    
    // AI接口限流：5 QPS
    private static final RateLimiter AI_LIMITER = RateLimiter.create(5.0);
    
    // 导出接口限流：1 QPS
    private static final RateLimiter EXPORT_LIMITER = RateLimiter.create(1.0);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        RateLimiter limiter = selectLimiter(uri);
        
        // 尝试获取令牌，超时时间500ms
        if (!limiter.tryAcquire(500, TimeUnit.MILLISECONDS)) {
            log.warn("接口限流，请求被拒绝: {}", uri);
            returnJson(response, Result.error(429, "请求过于频繁，请稍后再试"));
            return false;
        }
        
        return true;
    }
    
    /**
     * 根据URI选择对应的限流器
     */
    private RateLimiter selectLimiter(String uri) {
        // AI接口限流更严格
        if (uri.contains("/ai/") || uri.contains("/api/ai")) {
            return AI_LIMITER;
        }
        // 导出接口限流最严格
        if (uri.contains("/export") || uri.contains("/report")) {
            return EXPORT_LIMITER;
        }
        // 看板接口
        return DASHBOARD_LIMITER;
    }

    /**
     * 返回JSON格式的错误响应
     */
    private void returnJson(HttpServletResponse response, Result<?> result) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(429);  // Too Many Requests
        
        PrintWriter writer = response.getWriter();
        writer.write(OBJECT_MAPPER.writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}