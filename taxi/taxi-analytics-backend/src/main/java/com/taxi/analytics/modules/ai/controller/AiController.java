package com.taxi.analytics.modules.ai.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.ai.client.ChatMessage;
import com.taxi.analytics.modules.ai.service.AiService;
import com.taxi.analytics.modules.ai.service.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI", description = "AI智能查询API")
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private final AiService aiService;
    
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "自然语言查询", description = "输入自然语言问题，返回SQL查询结果和可视化配置")
    @PostMapping("/chat")
    public Result<AiQueryResponse> chat(@Valid @RequestBody AiQueryRequest request) {
        log.info("Received AI chat request: {}", request.getQuery());
        return Result.success(aiService.processQuery(request));
    }

    @Operation(summary = "生成ETL SQL", description = "根据自然语言描述生成Spark SQL ETL语句")
    @PostMapping("/gen-etl")
    public Result<EtlGenResponse> generateEtl(@Valid @RequestBody EtlGenRequest request) {
        log.info("Received ETL generation request: {}", request.getDescription());
        return Result.success(aiService.generateEtlSql(request));
    }

    @Operation(summary = "字段映射推荐", description = "源表到目标表的字段映射推荐")
    @PostMapping("/map-fields")
    public Result<FieldMappingResponse> mapFields(@Valid @RequestBody FieldMapRequest request) {
        log.info("Received field mapping request: source={}, target={}",
                 request.getSourceTable(), request.getTargetTable());
        return Result.success(aiService.mapFields(request));
    }

    @Operation(summary = "数据倾斜诊断", description = "分析Spark执行计划，诊断数据倾斜问题")
    @PostMapping("/skew-diagnose")
    public Result<SkewDiagnoseResponse> diagnoseSkew(@Valid @RequestBody SkewDiagnoseRequest request) {
        log.info("Received skew diagnose request for job: {}", request.getJobId());
        return Result.success(aiService.diagnoseSkew(request));
    }

    @Operation(summary = "失败任务根因分析", description = "解析DolphinScheduler日志，分析失败原因")
    @PostMapping("/diagnose-task")
    public Result<TaskDiagnoseResponse> diagnoseTask(@Valid @RequestBody TaskDiagnoseRequest request) {
        log.info("Received task diagnose request for task: {}", request.getTaskId());
        return Result.success(aiService.diagnoseTask(request));
    }

    @Operation(summary = "Flink反压诊断", description = "分析Flink作业反压情况")
    @PostMapping("/flink/backpressure")
    public Result<FlinkBackpressureResponse> diagnoseBackpressure(@Valid @RequestBody FlinkBackpressureRequest request) {
        log.info("Received Flink backpressure diagnosis request for job: {}", request.getJobId());
        return Result.success(aiService.diagnoseFlinkBackpressure(request));
    }

    @Operation(summary = "并行度推荐", description = "基于吞吐量推荐Flink作业并行度")
    @PostMapping("/flink/parallelism")
    public Result<ParallelismRecommendResponse> recommendParallelism(@Valid @RequestBody ParallelismRecommendRequest request) {
        log.info("Received parallelism recommendation request");
        return Result.success(aiService.recommendParallelism(request));
    }

    @Operation(summary = "智能规则生成", description = "根据数据分布推荐质量规则阈值")
    @PostMapping("/suggest-rules")
    public Result<SuggestRulesResponse> suggestRules(@Valid @RequestBody SuggestRulesRequest request) {
        log.info("Received suggest rules request for table: {}", request.getTableName());
        return Result.success(aiService.suggestQualityRules(request));
    }

    @Operation(summary = "告警自然语言化", description = "将技术告警转换为可读建议")
    @PostMapping("/alert/normalize")
    public Result<AlertNormalizeResponse> normalizeAlert(@Valid @RequestBody AlertNormalizeRequest request) {
        log.info("Received alert normalization request: {}", request.getAlertContent());
        return Result.success(aiService.normalizeAlert(request));
    }

    @Operation(summary = "会话历史查询")
    @GetMapping("/session/{sessionId}")
    public Result<List<ChatMessage>> getSessionHistory(@PathVariable String sessionId) {
        return Result.success(aiService.getSessionHistory(sessionId));
    }
}