package com.taxi.analytics.modules.ai.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM响应统一格式DTO
 * 用于解析智能体返回的多种格式响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {

    /**
     * 响应类型枚举
     */
    public enum ResponseType {
        DSL,      // 简单查询 - 返回标准DSL JSON
        SQL,      // 复杂查询 - 返回SQL + chart_config
        ERROR     // 解析失败
    }

    /**
     * 响应类型
     */
    private ResponseType type;

    /**
     * DSL模式下的DSL对象
     */
    private Map<String, Object> dsl;

    /**
     * SQL模式下的SQL语句
     */
    private String sql;

    /**
     * SQL模式下的业务解释文字
     */
    private String explanation;

    /**
     * SQL模式下的图表配置
     */
    private ChartConfig chartConfig;

    /**
     * 是否解析失败
     */
    @JsonProperty("parse_failed")
    private Boolean parseFailed;

    /**
     * 错误信息
     */
    @JsonProperty("error_msg")
    private String errorMsg;

    /**
     * 创建DSL类型响应
     */
    public static LlmResponse dsl(Map<String, Object> dsl) {
        return LlmResponse.builder()
                .type(ResponseType.DSL)
                .dsl(dsl)
                .build();
    }

    /**
     * 创建SQL类型响应
     */
    public static LlmResponse sql(String sql, String explanation, ChartConfig chartConfig) {
        return LlmResponse.builder()
                .type(ResponseType.SQL)
                .sql(sql)
                .explanation(explanation)
                .chartConfig(chartConfig)
                .build();
    }

    /**
     * 创建错误响应
     */
    public static LlmResponse error(String errorMsg) {
        return LlmResponse.builder()
                .type(ResponseType.ERROR)
                .parseFailed(true)
                .errorMsg(errorMsg)
                .build();
    }

    /**
     * 判断是否为DSL响应
     */
    public boolean isDslResponse() {
        return ResponseType.DSL.equals(type) || 
               (dsl != null && !dsl.isEmpty() && sql == null);
    }

    /**
     * 判断是否为SQL响应
     */
    public boolean isSqlResponse() {
        return ResponseType.SQL.equals(type) || 
               (sql != null && !sql.isEmpty());
    }

    /**
     * 判断是否为错误响应
     */
    public boolean isErrorResponse() {
        return ResponseType.ERROR.equals(type) || 
               Boolean.TRUE.equals(parseFailed);
    }
}