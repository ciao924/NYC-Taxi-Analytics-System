package com.taxi.analytics.modules.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.analytics.modules.ai.client.LLMClient;
import com.taxi.analytics.modules.ai.client.ChatMessage;
import com.taxi.analytics.modules.ai.dsl.Dsl;
import com.taxi.analytics.modules.ai.dsl.DslConverter;
import com.taxi.analytics.modules.ai.executor.QueryExecutor;
import com.taxi.analytics.modules.ai.guard.SqlSecurityGuard;
import com.taxi.analytics.modules.ai.intent.IntentClassifier;
import com.taxi.analytics.modules.ai.service.AiQueryTraceLogger;
import com.taxi.analytics.modules.ai.service.AiService;
import com.taxi.analytics.modules.ai.service.AiSessionService;
import com.taxi.analytics.modules.ai.service.LlmService;
import com.taxi.analytics.modules.ai.service.SqlGenerator;
import com.taxi.analytics.modules.ai.service.SchemaRetriever;
import com.taxi.analytics.modules.ai.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI服务实现类
 * 严格遵循AI智能助手模块前后端开发规范
 * 
 * 核心原则：
 * 1. 智能体是唯一的 SQL 和图表配置来源
 * 2. 后端不得修改智能体返回的 sql 和 chart_config（仅可补充 data 字段）
 * 3. 全链路可追溯
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final LlmService llmService;
    private final LLMClient llmClient;
    private final IntentClassifier intentClassifier;
    private final SqlGenerator sqlGenerator;
    private final SchemaRetriever schemaRetriever;
    private final DslConverter dslConverter;
    private final QueryExecutor queryExecutor;
    private final SqlSecurityGuard sqlSecurityGuard;
    private final AiQueryTraceLogger traceLogger;
    private final AiSessionService aiSessionService;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(LlmService llmService, LLMClient llmClient,
                         IntentClassifier intentClassifier, SqlGenerator sqlGenerator,
                         SchemaRetriever schemaRetriever, DslConverter dslConverter,
                         QueryExecutor queryExecutor, SqlSecurityGuard sqlSecurityGuard,
                         AiQueryTraceLogger traceLogger, AiSessionService aiSessionService) {
        this.llmService = llmService;
        this.llmClient = llmClient;
        this.intentClassifier = intentClassifier;
        this.sqlGenerator = sqlGenerator;
        this.schemaRetriever = schemaRetriever;
        this.dslConverter = dslConverter;
        this.queryExecutor = queryExecutor;
        this.sqlSecurityGuard = sqlSecurityGuard;
        this.traceLogger = traceLogger;
        this.aiSessionService = aiSessionService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 统一查询入口 - 智能路由判断
     * 严格遵循API契约格式返回响应
     */
    @Override
    public AiQueryResponse processQuery(AiQueryRequest request) {
        String sessionId = request.getSessionId() != null && !request.getSessionId().isEmpty()
            ? request.getSessionId()
            : UUID.randomUUID().toString();

        String userQuery = request.getQuery();
        String traceId = UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();
        String agentRawResponse = null;
        String executedSql = null;
        int rowCount = 0;
        boolean success = false;
        String errorMessage = null;
        AiQueryResponse response = null;

        try {
            traceLogger.logQueryStart(traceId, userQuery);

            LlmResponse llmResponse = llmService.callAgent(userQuery, sessionId);
            agentRawResponse = llmResponse.toString();
            traceLogger.logAgentResponse(traceId, agentRawResponse);

            log.info("智能体响应类型: {}", llmResponse.getType());

            if (llmResponse.isErrorResponse()) {
                errorMessage = llmResponse.getErrorMsg();
                response = handleErrorResponse(traceId, userQuery, errorMessage, startTime);
            } else if (llmResponse.isSqlResponse()) {
                response = handleSqlResponse(traceId, userQuery, llmResponse, startTime);
                executedSql = llmResponse.getSql();
                success = true;
            } else {
                response = handleDslResponse(traceId, userQuery, llmResponse, startTime);
                executedSql = response.getData().getSql();
                success = true;
            }

            if (response.getData() != null &&
                response.getData().getChartConfig() != null &&
                response.getData().getChartConfig().getData() != null) {
                rowCount = response.getData().getChartConfig().getData().size();
            }

            return response;

        } catch (Exception e) {
            errorMessage = "查询处理失败: " + e.getMessage();
            log.error("[{}] 查询处理失败: {}", traceId, errorMessage, e);
            response = AiQueryResponse.error(errorMessage);
            return response;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            traceLogger.logTrace(userQuery, agentRawResponse, executedSql,
                    rowCount, executionTime, success, errorMessage);
            traceLogger.logQueryComplete(traceId, success, errorMessage);

            storeSession(sessionId, userQuery, response, executedSql);
        }
    }

    /**
     * 处理错误响应
     */
    private AiQueryResponse handleErrorResponse(String traceId, String query, String errorMsg, long startTime) {
        log.warn("[{}] 智能体返回错误: {}", traceId, errorMsg);
        
        // 根据错误类型返回不同的错误码
        if (errorMsg.contains("请明确") || errorMsg.contains("缺少") || errorMsg.contains("无效")) {
            return AiQueryResponse.badRequest(errorMsg);
        }
        return AiQueryResponse.error(errorMsg);
    }

    /**
     * 处理SQL响应（新流程）
     * 直接使用LLM返回的SQL和chart_config，跳过DSL转换和SQL生成步骤
     * 
     * 核心流程：
     * 1. 获取LLM返回的SQL和chartConfig
     * 2. SQL安全校验
     * 3. 执行SQL查询
     * 4. 将数据注入chartConfig.data（关键步骤）
     * 5. 返回响应
     * 
     * 严格遵循规范：
     * - 原样保留智能体返回的sql和chart_config
     * - 仅补充data字段，不得修改其他字段名、值或结构
     */
    private AiQueryResponse handleSqlResponse(String traceId, String query, LlmResponse llmResponse, long startTime) {
        log.info("[{}] 处理SQL模式响应", traceId);
        
        String sql = llmResponse.getSql();
        String explanation = llmResponse.getExplanation();
        ChartConfig chartConfig = llmResponse.getChartConfig();
        
        log.info("[{}] LLM返回的SQL: {}", traceId, sql);
        log.info("[{}] LLM返回的解释: {}", traceId, explanation);
        log.info("[{}] LLM返回的chartConfig: {}", traceId, chartConfig != null ? "存在" : "null");

        // 1. SQL安全校验
        String validationError = sqlSecurityGuard.validate(sql);
        if (validationError != null) {
            traceLogger.logSqlValidation(traceId, sql, false, validationError);
            log.warn("[{}] SQL安全校验失败: {}", traceId, validationError);
            return AiQueryResponse.badRequest("SQL安全校验失败: " + validationError);
        }
        traceLogger.logSqlValidation(traceId, sql, true, null);

        // 2. 执行SQL查询
        List<Map<String, Object>> data;
        try {
            data = queryExecutor.execute(sql);
        } catch (Exception e) {
            log.error("[{}] SQL执行失败: {}", traceId, e.getMessage(), e);
            return AiQueryResponse.error("数据查询失败: " + e.getMessage());
        }
        log.info("[{}] SQL查询结果数量: {}", traceId, data.size());
        
        long executionTime = System.currentTimeMillis() - startTime;
        traceLogger.logSqlExecution(traceId, sql, data.size(), executionTime);

        // 3. 严格遵循规范：智能体必须返回chart_config，否则返回错误
        if (chartConfig == null) {
            log.error("[{}] 智能体未返回chart_config，违反规范", traceId);
            return AiQueryResponse.badRequest("智能体返回的响应缺少chart_config，请重新提问");
        }
        
        // 严格遵循规范：原样保留智能体返回的chart_config，不做任何修改
        // 仅补充data字段
        log.info("[{}] 原样保留智能体返回的chart_config", traceId);

        // 4. 将查询数据填充到chart_config.data中（关键步骤）
        // 严格遵循规范：后端不得修改chart_config，仅可补充data字段
        List<Map<String, Object>> chartData = convertToChartData(data, chartConfig);
        chartConfig.setData(chartData);
        
        log.info("[{}] 已将查询数据填充到chartConfig.data，数据量: {}", traceId, chartData.size());

        return AiQueryResponse.success(sql, chartConfig, executionTime, explanation);
    }

    /**
     * 处理DSL响应（旧流程兼容）
     * 走原有的DSL转换 -> SQL生成 -> 查询流程
     */
    private AiQueryResponse handleDslResponse(String traceId, String query, LlmResponse llmResponse, long startTime) {
        log.info("[{}] 处理DSL模式响应", traceId);
        
        Map<String, Object> dslMap = llmResponse.getDsl();
        
        try {
            // 1. 将Map转换为Dsl对象
            Dsl dsl = objectMapper.convertValue(dslMap, Dsl.class);
            log.info("[{}] DSL转换结果: {}", traceId, dsl);
            
            // 2. 验证并优化DSL
            dsl = dslConverter.validateAndOptimize(dsl);
            
            // 3. 根据DSL生成SQL
            String sql = sqlGenerator.generateSqlFromDsl(dsl);
            log.info("[{}] 生成SQL: {}", traceId, sql);
            
            // 4. SQL安全校验
            String validationError = sqlSecurityGuard.validate(sql);
            if (validationError != null) {
                traceLogger.logSqlValidation(traceId, sql, false, validationError);
                log.warn("[{}] SQL安全校验失败: {}", traceId, validationError);
                return AiQueryResponse.badRequest("SQL安全校验失败: " + validationError);
            }
            traceLogger.logSqlValidation(traceId, sql, true, null);
            
            // 5. 执行SQL查询
            List<Map<String, Object>> data = queryExecutor.execute(sql);
            log.info("[{}] 查询结果数量: {}", traceId, data.size());
            
            long executionTime = System.currentTimeMillis() - startTime;
            traceLogger.logSqlExecution(traceId, sql, data.size(), executionTime);
            
            // 6. 根据DSL生成图表配置
            ChartConfig chartConfig = sqlGenerator.suggestChartFromDsl(dsl);
            
            // 7. 设置图表数据（关键步骤）
            List<Map<String, Object>> chartData = convertToChartData(data, chartConfig);
            chartConfig.setData(chartData);

            return AiQueryResponse.success(sql, chartConfig, executionTime);
            
        } catch (Exception e) {
            log.error("[{}] 处理DSL响应失败: {}", traceId, e.getMessage(), e);
            return AiQueryResponse.error("处理DSL响应失败: " + e.getMessage());
        }
    }

    /**
     * 将查询结果转换为图表数据格式
     * 严格按照chartConfig中的x_field和y_field从data中提取对应数据
     * 
     * 严格遵循规范：
     * - 数据必须来自SQL查询结果
     * - data中的对象字段必须与x_field、y_field匹配
     * - 不进行任何字段猜测，严格按照配置提取
     * - 如果x_field或y_field为空，抛出异常
     */
    private List<Map<String, Object>> convertToChartData(List<Map<String, Object>> data, ChartConfig chartConfig) {
        List<Map<String, Object>> chartData = new ArrayList<>();
        
        String xField = chartConfig.getXField();
        Object yFieldObj = chartConfig.getYField();
        
        // 严格校验：x_field不能为空
        if (xField == null || xField.isEmpty()) {
            throw new IllegalArgumentException("chart_config中的x_field不能为空");
        }
        
        // 严格校验：y_field不能为空
        if (yFieldObj == null) {
            throw new IllegalArgumentException("chart_config中的y_field不能为空");
        }
        
        // 处理Y轴字段（可能是单个字段名或字段名数组）
        List<String> yFields = new ArrayList<>();
        if (yFieldObj instanceof String) {
            String yField = (String) yFieldObj;
            if (yField.isEmpty()) {
                throw new IllegalArgumentException("chart_config中的y_field不能为空字符串");
            }
            yFields.add(yField);
        } else if (yFieldObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> yFieldList = (List<String>) yFieldObj;
            if (yFieldList.isEmpty()) {
                throw new IllegalArgumentException("chart_config中的y_field数组不能为空");
            }
            yFields.addAll(yFieldList);
        } else {
            throw new IllegalArgumentException("chart_config中的y_field格式错误，必须是字符串或字符串数组");
        }
        
        log.info("图表数据转换 - xField: {}, yFields: {}, 数据量: {}", xField, yFields, data.size());
        
        for (Map<String, Object> row : data) {
            Map<String, Object> item = new HashMap<>();
            
            // 获取X轴值 - 严格使用x_field，不进行猜测
            Object xValue = row.get(xField);
            item.put(xField, xValue);
            
            // 获取Y轴值（支持多个Y字段）- 严格使用y_field，不进行猜测
            for (String yField : yFields) {
                Object yValue = row.get(yField);
                item.put(yField, yValue);
            }
            
            chartData.add(item);
        }
        
        log.info("图表数据转换完成，共 {} 条数据", chartData.size());
        return chartData;
    }

    /**
     * 存储会话记录
     */
    private void storeSession(String sessionId, String userQuery, AiQueryResponse response, String executedSql) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            List<Map<String, Object>> sessions = aiSessionService.getSessions();
            boolean sessionExists = sessions.stream().anyMatch(s -> sessionId.equals(s.get("session_id")));
            if (!sessionExists) {
                aiSessionService.createSession(sessionId, "新对话");
            }

            String chartDataJson = null;
            Integer executionTimeMs = null;
            Integer rowCount = null;
            if (response != null && response.getData() != null) {
                AiQueryResponse.ResponseData data = response.getData();
                if (data.getChartConfig() != null && data.getChartConfig().getData() != null) {
                    try {
                        chartDataJson = objectMapper.writeValueAsString(data.getChartConfig());
                    } catch (Exception e) {
                        log.warn("Failed to serialize chart config", e);
                    }
                    rowCount = data.getChartConfig().getData().size();
                }
                if (data.getExecutionTime() != null) {
                    executionTimeMs = (int) (long) data.getExecutionTime();
                }
            }

            aiSessionService.saveMessage(
                sessionId,
                "user",
                userQuery,
                executedSql,
                chartDataJson,
                executionTimeMs,
                rowCount
            );

            if (response != null) {
                String assistantContent = response.getCode() == 200
                    ? (response.getData() != null ? objectMapper.writeValueAsString(response.getData()) : "查询成功")
                    : response.getMessage();

                aiSessionService.saveMessage(
                    sessionId,
                    "assistant",
                    assistantContent,
                    executedSql,
                    chartDataJson,
                    executionTimeMs,
                    rowCount
                );
            }
        } catch (Exception e) {
            log.error("Failed to store session to database", e);
        }
    }

    // ========== 以下为原有功能实现（保持不变） ==========

    @Override
    public EtlGenResponse generateEtlSql(EtlGenRequest request) {
        String prompt = "你是一个Spark ETL专家。请根据以下信息生成ETL SQL:\n\n" +
            "源表: " + request.getSourceTable() + "\n" +
            "目标表: " + request.getTargetTable() + "\n" +
            "需求描述: " + request.getDescription() + "\n\n" +
            "请生成完整的Spark SQL INSERT语句，包括:\n" +
            "1. 字段映射和转换逻辑\n" +
            "2. 必要的SQL函数\n" +
            "3. 注释说明每个字段的转换规则";

        String result = llmClient.chatSync(prompt);
        EtlGenResponse response = new EtlGenResponse();

        if (result != null && !result.isEmpty()) {
            response.setSparkSql(result);
            response.setExplanation("已生成ETL SQL");
            response.setFieldMappings(new ArrayList<>());
        } else {
            response.setSparkSql("");
            response.setExplanation("ETL生成失败");
            response.setFieldMappings(new ArrayList<>());
        }
        return response;
    }

    @Override
    public FieldMappingResponse mapFields(FieldMapRequest request) {
        String prompt = "请为源表到目标表的字段映射提供推荐:\n\n" +
            "源表: " + request.getSourceTable() + "\n" +
            "目标表: " + request.getTargetTable() + "\n\n" +
            "请分析字段名称和类型，推荐最佳映射关系。";

        String result = llmClient.chatSync(prompt);
        FieldMappingResponse response = new FieldMappingResponse();

        if (result != null && !result.isEmpty()) {
            List<FieldMapping> mappings = parseFieldMappings(result);
            response.setMappings(mappings);
            response.setConfidence(0.85);
        } else {
            response.setMappings(new ArrayList<>());
            response.setConfidence(0.0);
        }
        return response;
    }

    private List<FieldMapping> parseFieldMappings(String text) {
        List<FieldMapping> mappings = new ArrayList<>();
        return mappings;
    }

    @Override
    public SkewDiagnoseResponse diagnoseSkew(SkewDiagnoseRequest request) {
        String prompt = "请分析以下Spark执行计划，诊断是否存在数据倾斜问题:\n\n" +
            "Job ID: " + request.getJobId() + "\n\n" +
            "执行计划:\n" + request.getExecutionPlan() + "\n\n" +
            "请提供:\n" +
            "1. 是否存在数据倾斜 (是/否)\n" +
            "2. 倾斜的Stage列表\n" +
            "3. 倾斜因子\n" +
            "4. 优化建议";

        String result = llmClient.chatSync(prompt);
        SkewDiagnoseResponse response = new SkewDiagnoseResponse();

        if (result != null && !result.isEmpty()) {
            response.setHasSkew(result.contains("是"));
            response.setSkewedStages(new ArrayList<>());
            response.setRecommendations(new ArrayList<>());
            response.setSkewFactor(0.0);
        } else {
            response.setHasSkew(false);
            response.setSkewedStages(new ArrayList<>());
            response.setRecommendations(new ArrayList<>());
            response.setSkewFactor(0.0);
        }
        return response;
    }

    @Override
    public TaskDiagnoseResponse diagnoseTask(TaskDiagnoseRequest request) {
        String prompt = "请分析以下DolphinScheduler任务日志，诊断失败原因:\n\n" +
            "Task ID: " + request.getTaskId() + "\n\n" +
            "日志内容:\n" + request.getTaskLog() + "\n\n" +
            "请提供:\n" +
            "1. 根因分析\n" +
            "2. 错误类别\n" +
            "3. 解决建议\n" +
            "4. 置信度";

        String result = llmClient.chatSync(prompt);
        TaskDiagnoseResponse response = new TaskDiagnoseResponse();

        if (result != null && !result.isEmpty()) {
            response.setRootCause(result);
            response.setCategory("UNKNOWN");
            response.setSuggestions(new ArrayList<>());
            response.setConfidence(0.8);
        } else {
            response.setRootCause("诊断失败");
            response.setCategory("ERROR");
            response.setSuggestions(new ArrayList<>());
            response.setConfidence(0.0);
        }
        return response;
    }

    @Override
    public FlinkBackpressureResponse diagnoseFlinkBackpressure(FlinkBackpressureRequest request) {
        StringBuilder metricsBuilder = new StringBuilder();
        if (request.getOperatorMetrics() != null) {
            for (String metric : request.getOperatorMetrics()) {
                metricsBuilder.append(metric).append("\n");
            }
        }

        String prompt = "请分析以下Flink作业的反压情况:\n\n" +
            "Job ID: " + request.getJobId() + "\n\n" +
            "算子指标:\n" + metricsBuilder.toString() + "\n" +
            "请提供:\n" +
            "1. 是否存在反压\n" +
            "2. 瓶颈算子\n" +
            "3. 反压比例\n" +
            "4. 优化建议";

        String result = llmClient.chatSync(prompt);
        FlinkBackpressureResponse response = new FlinkBackpressureResponse();

        if (result != null && !result.isEmpty()) {
            response.setHasBackpressure(result.contains("存在"));
            response.setBottleneckOperator("");
            response.setBackpressureRatio(0.0);
            response.setRecommendations(new ArrayList<>());
        } else {
            response.setHasBackpressure(false);
            response.setBottleneckOperator("");
            response.setBackpressureRatio(0.0);
            response.setRecommendations(new ArrayList<>());
        }
        return response;
    }

    @Override
    public ParallelismRecommendResponse recommendParallelism(ParallelismRecommendRequest request) {
        String prompt = "请基于以下信息推荐Flink作业并行度:\n\n" +
            "Job ID: " + request.getJobId() + "\n" +
            "当前吞吐量: " + request.getCurrentThroughput() + "\n" +
            "当前并行度: " + request.getCurrentParallelism() + "\n\n" +
            "请提供:\n" +
            "1. 推荐并行度\n" +
            "2. 预期改善效果\n" +
            "3. 各算子并行度建议";

        String result = llmClient.chatSync(prompt);
        ParallelismRecommendResponse response = new ParallelismRecommendResponse();

        if (result != null && !result.isEmpty()) {
            response.setRecommendedParallelism(request.getCurrentParallelism() * 2);
            response.setExpectedImprovement(30.0);
            response.setOperatorRecommendations(new ArrayList<>());
        } else {
            response.setRecommendedParallelism(request.getCurrentParallelism());
            response.setExpectedImprovement(0.0);
            response.setOperatorRecommendations(new ArrayList<>());
        }
        return response;
    }

    @Override
    public SuggestRulesResponse suggestQualityRules(SuggestRulesRequest request) {
        StringBuilder samplesBuilder = new StringBuilder();
        if (request.getColumnSamples() != null) {
            for (String sample : request.getColumnSamples()) {
                samplesBuilder.append(sample).append("\n");
            }
        }

        String prompt = "请基于以下数据样本推荐数据质量规则:\n\n" +
            "表名: " + request.getTableName() + "\n\n" +
            "列样本:\n" + samplesBuilder.toString() + "\n" +
            "请提供推荐的质量规则，包括:\n" +
            "1. 规则类型 (非空、唯一性、数值范围等)\n" +
            "2. 适用字段\n" +
            "3. 阈值";

        String result = llmClient.chatSync(prompt);
        SuggestRulesResponse response = new SuggestRulesResponse();

        if (result != null && !result.isEmpty()) {
            List<QualityRule> qualityRules = new ArrayList<>();
            response.setSuggestedRules(qualityRules);
            response.setConfidence(0.75);
        } else {
            response.setSuggestedRules(new ArrayList<>());
            response.setConfidence(0.0);
        }
        return response;
    }

    @Override
    public AlertNormalizeResponse normalizeAlert(AlertNormalizeRequest request) {
        String prompt = "请将以下技术告警转换为可读的描述和建议:\n\n" +
            "告警内容: " + request.getAlertContent() + "\n" +
            "告警类型: " + request.getAlertType() + "\n\n" +
            "请提供:\n" +
            "1. 告警的通俗解释\n" +
            "2. 严重程度\n" +
            "3. 建议的处理动作";

        String result = llmClient.chatSync(prompt);
        AlertNormalizeResponse response = new AlertNormalizeResponse();

        if (result != null && !result.isEmpty()) {
            response.setNormalizedAlert(result);
            response.setSeverity("MEDIUM");
            response.setSuggestedActions(new ArrayList<>());
        } else {
            response.setNormalizedAlert("告警转换失败");
            response.setSeverity("UNKNOWN");
            response.setSuggestedActions(new ArrayList<>());
        }
        return response;
    }

    @Override
    public List<ChatMessage> getSessionHistory(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        try {
            List<Map<String, Object>> dbMessages = aiSessionService.getSessionMessages(sessionId);
            log.info("从数据库加载会话历史，会话ID: {}, 消息数量: {}", sessionId, dbMessages.size());
            for (Map<String, Object> msg : dbMessages) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setMessageId(msg.get("message_id") != null ? msg.get("message_id").toString() : null);
                chatMessage.setRole(msg.get("role") != null ? msg.get("role").toString() : "assistant");

                Object contentObj = msg.get("content");
                String content = contentObj != null ? contentObj.toString() : "";
                chatMessage.setContent(content);

                String role = chatMessage.getRole();

                // 优先从 chart_data 字段解析 chartConfig
                Object chartDataObj = msg.get("chart_data");
                Map<String, Object> chartConfig = null;
                if (chartDataObj != null) {
                    String chartDataStr = null;
                    if (chartDataObj instanceof String) {
                        chartDataStr = (String) chartDataObj;
                    } else if (chartDataObj instanceof Map) {
                        // 如果已经是 Map，直接使用
                        @SuppressWarnings("unchecked")
                        Map<String, Object> chartDataMap = (Map<String, Object>) chartDataObj;
                        chartConfig = chartDataMap;
                        log.debug("chart_data 已经是 Map 类型，直接使用");
                    }
                    if (chartDataStr != null && !chartDataStr.isEmpty() && !"null".equals(chartDataStr)) {
                        try {
                            chartConfig = objectMapper.readValue(chartDataStr, Map.class);
                            log.debug("从 chart_data 解析 chartConfig 成功");
                        } catch (Exception e) {
                            log.warn("从 chart_data 解析 chartConfig 失败: {}", e.getMessage());
                        }
                    }
                }

                // 如果 chartConfig 为空或缺少 data 字段，尝试从 content 中解析
                if (chartConfig == null || !chartConfig.containsKey("data")) {
                    if ("assistant".equals(role) && content.startsWith("{")) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsedContent = objectMapper.readValue(content, Map.class);
                            if (parsedContent.containsKey("chartConfig")) {
                                Object chartConfigFromContent = parsedContent.get("chartConfig");
                                if (chartConfigFromContent instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> cc = (Map<String, Object>) chartConfigFromContent;
                                    if (chartConfig == null || !chartConfig.containsKey("data")) {
                                        chartConfig = cc;
                                        log.debug("从 content 解析 chartConfig 成功");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("从 content 解析 chartConfig 失败: {}", e.getMessage());
                        }
                    }
                }

                chatMessage.setChartConfig(chartConfig);

                // 从 content 或 chartConfig 中解析 explanation
                if ("assistant".equals(role) && content.startsWith("{")) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsedContent = objectMapper.readValue(content, Map.class);
                        if (parsedContent.containsKey("explanation")) {
                            chatMessage.setExplanation(parsedContent.get("explanation").toString());
                        }
                    } catch (Exception e) {
                        log.warn("从 content 解析 explanation 失败: {}", e.getMessage());
                    }
                }

                chatMessage.setSql(msg.get("sql_text") != null ? msg.get("sql_text").toString() : null);
                chatMessage.setExecutionTime(msg.get("execution_time_ms") != null ? ((Number) msg.get("execution_time_ms")).intValue() : null);

                // 调试日志
                if (log.isDebugEnabled()) {
                    log.debug("消息加载 - messageId: {}, role: {}, hasChartConfig: {}, chartConfigKeys: {}",
                            chatMessage.getMessageId(), chatMessage.getRole(),
                            chartConfig != null, chartConfig != null ? chartConfig.keySet() : "N/A");
                }

                long timestamp = 0;
                if (msg.get("create_time") != null) {
                    if (msg.get("create_time") instanceof LocalDateTime) {
                        timestamp = ((LocalDateTime) msg.get("create_time")).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    } else if (msg.get("create_time") instanceof java.sql.Timestamp) {
                        timestamp = ((java.sql.Timestamp) msg.get("create_time")).getTime();
                    }
                }
                chatMessage.setTimestamp(timestamp);

                messages.add(chatMessage);
            }
        } catch (Exception e) {
            log.error("从数据库获取会话历史失败", e);
        }
        return messages;
    }
}