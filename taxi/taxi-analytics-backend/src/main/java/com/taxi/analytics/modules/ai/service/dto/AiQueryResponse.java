package com.taxi.analytics.modules.ai.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI 查询响应 DTO
 * 严格遵循 API 契约格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQueryResponse {

    /**
     * 响应码
     * 200: 成功
     * 400: 请求参数错误 / 智能体解析失败
     * 500: 服务器内部错误 / SQL 执行失败
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private ResponseData data;

    /**
     * 响应数据内部结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseData {
        
        /**
         * 执行的 SQL 语句
         */
        private String sql;

        /**
         * 图表配置信息
         */
        private ChartConfig chartConfig;

        /**
         * SQL 执行耗时（毫秒）
         */
        private Long executionTime;

        /**
         * 业务解释文字（可选）
         */
        private String explanation;

        /**
         * 查询结果数量
         */
        private Integer count;
    }

    /**
     * 创建成功响应
     */
    public static AiQueryResponse success(String sql, ChartConfig chartConfig, Long executionTime) {
        return AiQueryResponse.builder()
                .code(200)
                .message("success")
                .data(ResponseData.builder()
                        .sql(sql)
                        .chartConfig(chartConfig)
                        .executionTime(executionTime)
                        .count(chartConfig != null && chartConfig.getData() != null 
                                ? chartConfig.getData().size() : 0)
                        .build())
                .build();
    }

    /**
     * 创建成功响应（带解释）
     */
    public static AiQueryResponse success(String sql, ChartConfig chartConfig, Long executionTime, String explanation) {
        return AiQueryResponse.builder()
                .code(200)
                .message("success")
                .data(ResponseData.builder()
                        .sql(sql)
                        .chartConfig(chartConfig)
                        .executionTime(executionTime)
                        .explanation(explanation)
                        .count(chartConfig != null && chartConfig.getData() != null 
                                ? chartConfig.getData().size() : 0)
                        .build())
                .build();
    }

    /**
     * 创建失败响应（400 错误）
     */
    public static AiQueryResponse badRequest(String errorMsg) {
        return AiQueryResponse.builder()
                .code(400)
                .message(errorMsg)
                .data(null)
                .build();
    }

    /**
     * 创建失败响应（500 错误）
     */
    public static AiQueryResponse error(String errorMsg) {
        return AiQueryResponse.builder()
                .code(500)
                .message(errorMsg)
                .data(null)
                .build();
    }

    /**
     * 创建失败响应（自定义错误码）
     */
    public static AiQueryResponse error(Integer code, String errorMsg) {
        return AiQueryResponse.builder()
                .code(code)
                .message(errorMsg)
                .data(null)
                .build();
    }
}