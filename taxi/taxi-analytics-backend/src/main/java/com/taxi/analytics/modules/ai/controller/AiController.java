package com.taxi.analytics.modules.ai.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.ai.client.ChatMessage;
import com.taxi.analytics.modules.ai.service.AiService;
import com.taxi.analytics.modules.ai.service.AiSessionService;
import com.taxi.analytics.modules.ai.service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI智能查询控制器
 * 提供统一的AI查询入口，严格遵循API契约格式
 * 
 * API 契约：
 * POST /api/ai/query
 * 请求体: { "query": "...", "sessionId": "..." }
 * 响应体: { "code": 200, "message": "success", "data": {...} }
 * 
 * 注意：context-path 已配置为 /api，所以实际路径为 /api/ai/xxx
 */
@Tag(name = "AI", description = "AI智能查询API")
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private final AiService aiService;
    private final AiSessionService aiSessionService;
    
    public AiController(AiService aiService, AiSessionService aiSessionService) {
        this.aiService = aiService;
        this.aiSessionService = aiSessionService;
    }

    /**
     * 统一查询入口 - 支持智能路由判断
     * 根据LLM返回的响应类型自动选择处理流程：
     * - DSL响应 → 走原有DSL转换流程
     * - SQL响应 → 直接使用LLM返回的SQL和chart_config
     * - 错误响应 → 返回友好错误提示
     * 
     * 返回格式严格遵循API契约：
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": {
     *     "sql": "...",
     *     "chartConfig": {...},
     *     "executionTime": 156
     *   }
     * }
     */
    @Operation(summary = "统一AI查询入口", description = "输入自然语言问题，智能路由处理，返回SQL查询结果和可视化配置")
    @PostMapping("/query")
    public AiQueryResponse query(@Valid @RequestBody AiQueryRequest request) {
        log.info("Received AI query request: {}", request.getQuery());
        return aiService.processQuery(request);
    }

    /**
     * 自然语言查询（兼容旧版API）
     * 保持与原有前端的兼容性
     */
    @Operation(summary = "自然语言查询（兼容旧版）", description = "输入自然语言问题，返回SQL查询结果和可视化配置")
    @PostMapping("/chat")
    public AiQueryResponse chat(@Valid @RequestBody AiQueryRequest request) {
        log.info("Received AI chat request: {}", request.getQuery());
        return aiService.processQuery(request);
    }

    @Operation(summary = "生成ETL SQL", description = "根据自然语言描述生成Spark SQL ETL语句")
    @PostMapping("/gen-etl")
    public EtlGenResponse generateEtl(@Valid @RequestBody EtlGenRequest request) {
        log.info("Received ETL generation request: {}", request.getDescription());
        return aiService.generateEtlSql(request);
    }

    @Operation(summary = "字段映射推荐", description = "源表到目标表的字段映射推荐")
    @PostMapping("/map-fields")
    public FieldMappingResponse mapFields(@Valid @RequestBody FieldMapRequest request) {
        log.info("Received field mapping request: source={}, target={}",
                 request.getSourceTable(), request.getTargetTable());
        return aiService.mapFields(request);
    }

    @Operation(summary = "数据倾斜诊断", description = "分析Spark执行计划，诊断数据倾斜问题")
    @PostMapping("/skew-diagnose")
    public SkewDiagnoseResponse diagnoseSkew(@Valid @RequestBody SkewDiagnoseRequest request) {
        log.info("Received skew diagnose request for job: {}", request.getJobId());
        return aiService.diagnoseSkew(request);
    }

    @Operation(summary = "失败任务根因分析", description = "解析DolphinScheduler日志，分析失败原因")
    @PostMapping("/diagnose-task")
    public TaskDiagnoseResponse diagnoseTask(@Valid @RequestBody TaskDiagnoseRequest request) {
        log.info("Received task diagnose request for task: {}", request.getTaskId());
        return aiService.diagnoseTask(request);
    }

    @Operation(summary = "Flink反压诊断", description = "分析Flink作业反压情况")
    @PostMapping("/flink/backpressure")
    public FlinkBackpressureResponse diagnoseBackpressure(@Valid @RequestBody FlinkBackpressureRequest request) {
        log.info("Received Flink backpressure diagnosis request for job: {}", request.getJobId());
        return aiService.diagnoseFlinkBackpressure(request);
    }

    @Operation(summary = "并行度推荐", description = "基于吞吐量推荐Flink作业并行度")
    @PostMapping("/flink/parallelism")
    public ParallelismRecommendResponse recommendParallelism(@Valid @RequestBody ParallelismRecommendRequest request) {
        log.info("Received parallelism recommendation request");
        return aiService.recommendParallelism(request);
    }

    @Operation(summary = "智能规则生成", description = "根据数据分布推荐质量规则阈值")
    @PostMapping("/suggest-rules")
    public SuggestRulesResponse suggestRules(@Valid @RequestBody SuggestRulesRequest request) {
        log.info("Received suggest rules request for table: {}", request.getTableName());
        return aiService.suggestQualityRules(request);
    }

    @Operation(summary = "告警自然语言化", description = "将技术告警转换为可读建议")
    @PostMapping("/alert/normalize")
    public AlertNormalizeResponse normalizeAlert(@Valid @RequestBody AlertNormalizeRequest request) {
        log.info("Received alert normalization request: {}", request.getAlertContent());
        return aiService.normalizeAlert(request);
    }

    @Operation(summary = "会话历史查询")
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessage>> getSessionHistory(@PathVariable String sessionId) {
        return Result.success(aiService.getSessionHistory(sessionId));
    }

    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> getSessions() {
        return Result.success(aiSessionService.getSessions());
    }

    @Operation(summary = "查询历史")
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getQueryHistory(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(aiSessionService.getQueryHistory(limit));
    }
}