package com.taxi.analytics.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI 查询全链路日志记录工具类
 * 记录每次请求的完整链路信息：用户问题 → 智能体原始响应 → 最终执行的 SQL → 查询耗时
 */
@Component
public class AiQueryTraceLogger {

    private static final Logger log = LoggerFactory.getLogger(AiQueryTraceLogger.class);
    private static final Logger traceLog = LoggerFactory.getLogger("AI_QUERY_TRACE");
    
    private final ObjectMapper objectMapper;

    public AiQueryTraceLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 记录完整的查询链路日志
     *
     * @param userQuery        用户原始查询问题
     * @param agentRawResponse 智能体原始响应
     * @param executedSql      最终执行的 SQL
     * @param rowCount         返回行数
     * @param executionTimeMs  执行耗时（毫秒）
     * @param success          是否成功
     * @param errorMessage     错误信息（失败时）
     * @return traceId
     */
    public String logTrace(String userQuery, String agentRawResponse, String executedSql,
                           int rowCount, long executionTimeMs, boolean success, String errorMessage) {
        String traceId = UUID.randomUUID().toString();
        
        Map<String, Object> traceInfo = new HashMap<>();
        traceInfo.put("traceId", traceId);
        traceInfo.put("userQuery", userQuery);
        traceInfo.put("agentRawResponse", agentRawResponse);
        traceInfo.put("executedSql", executedSql);
        traceInfo.put("rowCount", rowCount);
        traceInfo.put("executionTimeMs", executionTimeMs);
        traceInfo.put("success", success);
        traceInfo.put("errorMessage", errorMessage);
        traceInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        try {
            String logJson = objectMapper.writeValueAsString(traceInfo);
            traceLog.info(logJson);
            log.info("AI查询链路日志已记录, traceId={}, success={}, executionTimeMs={}", 
                    traceId, success, executionTimeMs);
        } catch (Exception e) {
            log.error("记录链路日志失败", e);
        }

        return traceId;
    }

    /**
     * 创建查询上下文（用于跟踪）
     */
    public Map<String, Object> createContext(String userQuery, String sessionId) {
        Map<String, Object> context = new HashMap<>();
        context.put("traceId", UUID.randomUUID().toString());
        context.put("userQuery", userQuery);
        context.put("sessionId", sessionId);
        context.put("startTime", System.currentTimeMillis());
        context.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return context;
    }

    /**
     * 记录查询开始
     */
    public void logQueryStart(String traceId, String userQuery) {
        log.info("[{}] AI查询开始: {}", traceId, userQuery);
    }

    /**
     * 记录智能体调用完成
     */
    public void logAgentResponse(String traceId, String agentResponse) {
        log.debug("[{}] 智能体响应: {}", traceId, truncate(agentResponse, 500));
    }

    /**
     * 记录 SQL 校验结果
     */
    public void logSqlValidation(String traceId, String sql, boolean valid, String errorMessage) {
        if (valid) {
            log.info("[{}] SQL校验通过", traceId);
        } else {
            log.warn("[{}] SQL校验失败: {}", traceId, errorMessage);
        }
    }

    /**
     * 记录 SQL 执行结果
     */
    public void logSqlExecution(String traceId, String sql, int rowCount, long executionTimeMs) {
        log.info("[{}] SQL执行完成: {} 行, {}ms", traceId, rowCount, executionTimeMs);
        log.debug("[{}] 执行的SQL: {}", traceId, sql);
    }

    /**
     * 记录查询完成
     */
    public void logQueryComplete(String traceId, boolean success, String errorMessage) {
        if (success) {
            log.info("[{}] AI查询完成", traceId);
        } else {
            log.error("[{}] AI查询失败: {}", traceId, errorMessage);
        }
    }

    /**
     * 截断字符串（用于日志输出）
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...(truncated)";
    }
}