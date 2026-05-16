package com.taxi.analytics.modules.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.analytics.modules.ai.client.CozeClient;
import com.taxi.analytics.modules.ai.service.LlmService;
import com.taxi.analytics.modules.ai.service.SchemaRetriever;
import com.taxi.analytics.modules.ai.service.dto.ChartConfig;
import com.taxi.analytics.modules.ai.service.dto.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM服务实现类
 * 负责调用智能体并解析多种格式的响应
 * 使用Coze平台作为智能体服务
 */
@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmServiceImpl.class);

    private final CozeClient cozeClient;
    private final ObjectMapper objectMapper;
    private final SchemaRetriever schemaRetriever;

    public LlmServiceImpl(CozeClient cozeClient, SchemaRetriever schemaRetriever) {
        this.cozeClient = cozeClient;
        this.objectMapper = new ObjectMapper();
        this.schemaRetriever = schemaRetriever;
    }

    @Override
    public LlmResponse callAgent(String query) {
        return callAgent(query, null);
    }

    @Override
    public LlmResponse callAgent(String query, String sessionId) {
        log.info("调用Coze智能体，查询: {}, sessionId: {}", query, sessionId);
        
        try {
            // 获取数据库schema信息，用于增强智能体的查询能力
            String schemaJson = null;
            try {
                Map<String, Object> schema = schemaRetriever.getSchema("nyc_taxi_ads");
                schemaJson = objectMapper.writeValueAsString(schema);
                log.debug("Schema信息长度: {}", schemaJson.length());
            } catch (Exception e) {
                log.warn("获取schema信息失败: {}", e.getMessage());
            }
            
            // 调用Coze客户端获取响应（携带schema信息）
            String rawResponse = cozeClient.callAIWithSchema(query, schemaJson);
            log.info("智能体响应长度: {}", rawResponse != null ? rawResponse.length() : 0);

            if (rawResponse == null || rawResponse.isEmpty()) {
                log.warn("智能体返回空响应");
                return LlmResponse.error("智能体未返回有效响应");
            }

            // 解析响应
            return parseResponse(rawResponse);
            
        } catch (Exception e) {
            log.error("调用智能体失败: {}", e.getMessage(), e);
            return LlmResponse.error("调用智能体失败: " + e.getMessage());
        }
    }

    /**
     * 解析智能体响应
     * 支持多种响应格式：
     * 1. SQL格式（优先）- {"sql": "...", "explanation": "...", "chart_config": {...}}
     * 2. 包装格式 - {"response": {"sql": "...", ...}}
     * 3. Content嵌套格式 - {"type": "content", "content": {"text": "{...}"}}
     * 4. Tool Result格式 - {"type": "tool_result", "content": {"tool_result": {...}}}
     * 5. Answer格式 - {"type": "answer", "answer": "{...}"}
     * 6. DSL格式 - {"metric": "...", "dimension": "...", ...}
     * 7. 错误格式 - {"parse_failed": true, "error_msg": "..."}
     */
    private LlmResponse parseResponse(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            log.error("智能体响应为空");
            return LlmResponse.error("智能体响应为空");
        }
        
        // 清理响应内容（移除可能的干扰字符）
        String cleanedResponse = cleanResponse(responseText);
        
        log.debug("开始解析智能体响应，长度: {}", cleanedResponse.length());
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(cleanedResponse);
            
            // ====== 1. 优先检查SQL格式响应 ======
            // 直接包含sql字段的格式
            if (root.has("sql")) {
                return parseSqlResponse(root, mapper);
            }
            
            // ====== 2. 检查包装格式的SQL响应 ======
            // {"response": {"sql": "...", ...}} 格式
            if (root.has("response")) {
                JsonNode responseNode = root.get("response");
                if (responseNode.has("sql")) {
                    return parseSqlResponse(responseNode, mapper);
                }
            }
            
            // ====== 3. 检查Content嵌套格式 ======
            // {"type": "content", "content": {"text": "{...}"}} 格式
            if ("content".equals(root.has("type") ? root.get("type").asText() : "") && root.has("content")) {
                LlmResponse nestedResponse = parseNestedContent(root.get("content"), mapper, cleanedResponse);
                if (nestedResponse != null) {
                    return nestedResponse;
                }
            }
            
            // ====== 4. 检查Answer格式 ======
            // {"type": "answer", "answer": "{...}"} 格式
            if ("answer".equals(root.has("type") ? root.get("type").asText() : "")) {
                LlmResponse nestedResponse = parseAnswerContent(root, mapper, cleanedResponse);
                if (nestedResponse != null) {
                    return nestedResponse;
                }
            }
            
            // ====== 5. 检查Tool Result格式 ======
            // {"type": "tool_result", "content": {"tool_result": {...}}} 格式
            if ("tool_result".equals(root.has("type") ? root.get("type").asText() : "")) {
                LlmResponse nestedResponse = parseToolResultContent(root, mapper, cleanedResponse);
                if (nestedResponse != null) {
                    return nestedResponse;
                }
            }
            
            // ====== 6. 检查错误响应格式 ======
            if (root.has("parse_failed") && root.get("parse_failed").asBoolean()) {
                String errorMsg = root.has("error_msg") ? root.get("error_msg").asText() : "解析失败";
                log.warn("智能体返回错误响应: {}", errorMsg);
                return LlmResponse.error(errorMsg);
            }
            
            // 检查其他错误格式
            if (root.has("error") || root.has("errorMessage") || root.has("message")) {
                String errorMsg = extractErrorMessage(root);
                log.warn("智能体返回错误响应: {}", errorMsg);
                return LlmResponse.error(errorMsg.isEmpty() ? "智能体返回错误" : errorMsg);
            }
            
            // ====== 7. 检查DSL格式响应 ======
            if (root.has("metric") || root.has("dimension") || root.has("timeRange")) {
                log.info("智能体返回DSL格式响应");
                // 将JsonNode转换为Map
                @SuppressWarnings("unchecked")
                Map<String, Object> dslMap = mapper.convertValue(root, Map.class);
                return LlmResponse.dsl(dslMap);
            }
            
            // ====== 8. 检查其他可能的嵌套格式 ======
            // 尝试从content字段提取嵌套内容
            if (root.has("content")) {
                LlmResponse nestedResponse = parseNestedContent(root.get("content"), mapper, cleanedResponse);
                if (nestedResponse != null) {
                    return nestedResponse;
                }
            }
            
            // ====== 9. 如果都不匹配，记录详细日志并返回错误 ======
            log.error("【错误】无法识别智能体响应格式");
            log.error("原始响应内容（前1000字符）: {}", cleanedResponse.length() > 1000 ? cleanedResponse.substring(0, 1000) + "..." : cleanedResponse);
            
            // 尝试解析响应中的文本内容作为错误消息
            String textContent = extractTextContent(root);
            if (textContent != null && !textContent.isEmpty()) {
                log.warn("响应中包含文本内容，作为错误消息返回: {}", textContent.length() > 100 ? textContent.substring(0, 100) + "..." : textContent);
                return LlmResponse.error(textContent);
            }
            
            return LlmResponse.error("无法识别智能体响应格式，请检查智能体配置");
            
        } catch (Exception e) {
            log.error("解析智能体响应失败: {}", e.getMessage(), e);
            log.error("原始响应内容（前1000字符）: {}", cleanedResponse.length() > 1000 ? cleanedResponse.substring(0, 1000) + "..." : cleanedResponse);
            
            // 如果JSON解析失败，尝试将响应作为纯文本处理
            if (!cleanedResponse.startsWith("{")) {
                log.warn("响应不是有效的JSON，作为纯文本错误消息返回");
                return LlmResponse.error(cleanedResponse.trim());
            }
            
            // 尝试查找响应中可能包含的JSON片段
            String embeddedJson = findEmbeddedJson(cleanedResponse);
            if (embeddedJson != null) {
                log.info("发现嵌入的JSON片段，尝试重新解析");
                return parseResponse(embeddedJson);
            }
            
            return LlmResponse.error("响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析SQL格式响应
     */
    private LlmResponse parseSqlResponse(JsonNode node, ObjectMapper mapper) {
        String sql = node.get("sql").asText();
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        
        String explanation = node.has("explanation") ? node.get("explanation").asText() : "";
        ChartConfig config = null;
        
        // 处理 chart_config 字段（支持下划线格式）
        if (node.has("chart_config")) {
            try {
                config = mapper.treeToValue(node.get("chart_config"), ChartConfig.class);
            } catch (Exception e) {
                log.warn("解析 chart_config 失败: {}", e.getMessage());
            }
        }
        // 兼容驼峰格式的 chartConfig
        else if (node.has("chartConfig")) {
            try {
                config = mapper.treeToValue(node.get("chartConfig"), ChartConfig.class);
            } catch (Exception e) {
                log.warn("解析 chartConfig 失败: {}", e.getMessage());
            }
        }
        
        log.info("智能体返回SQL格式响应，SQL长度: {}, 包含chartConfig: {}", sql.length(), config != null);
        return LlmResponse.sql(sql, explanation, config);
    }

    /**
     * 解析嵌套的content内容
     */
    private LlmResponse parseNestedContent(JsonNode contentNode, ObjectMapper mapper, String originalResponse) {
        if (contentNode == null) return null;
        
        // content是字符串，可能是JSON
        if (contentNode.isTextual()) {
            String textContent = contentNode.asText().trim();
            if (textContent.startsWith("{") && textContent.endsWith("}")) {
                log.info("发现嵌套的JSON内容（文本类型）");
                return parseResponse(textContent);
            }
            return null;
        }
        
        // content是对象，检查是否有text字段包含JSON
        if (contentNode.isObject()) {
            if (contentNode.has("text")) {
                String textContent = contentNode.get("text").asText().trim();
                if (textContent.startsWith("{") && textContent.endsWith("}")) {
                    log.info("发现嵌套的JSON内容（text字段）");
                    return parseResponse(textContent);
                }
            }
            
            // 检查是否直接包含sql字段
            if (contentNode.has("sql")) {
                return parseSqlResponse(contentNode, mapper);
            }
        }
        
        return null;
    }

    /**
     * 解析Answer格式内容
     */
    private LlmResponse parseAnswerContent(JsonNode root, ObjectMapper mapper, String originalResponse) {
        // 检查answer字段
        if (root.has("answer")) {
            JsonNode answerNode = root.get("answer");
            if (answerNode.isTextual()) {
                String answerText = answerNode.asText().trim();
                if (answerText.startsWith("{") && answerText.endsWith("}")) {
                    log.info("发现answer字段中的JSON内容");
                    return parseResponse(answerText);
                }
            } else if (answerNode.isObject() && answerNode.has("sql")) {
                return parseSqlResponse(answerNode, mapper);
            }
        }
        
        // 检查content字段
        if (root.has("content")) {
            return parseNestedContent(root.get("content"), mapper, originalResponse);
        }
        
        return null;
    }

    /**
     * 解析Tool Result格式内容
     */
    private LlmResponse parseToolResultContent(JsonNode root, ObjectMapper mapper, String originalResponse) {
        if (root.has("content")) {
            JsonNode contentNode = root.get("content");
            
            // 检查tool_result字段
            if (contentNode.has("tool_result")) {
                JsonNode toolResultNode = contentNode.get("tool_result");
                if (toolResultNode.isTextual()) {
                    String toolResultText = toolResultNode.asText().trim();
                    if (toolResultText.startsWith("{") && toolResultText.endsWith("}")) {
                        log.info("发现tool_result字段中的JSON内容");
                        return parseResponse(toolResultText);
                    }
                } else if (toolResultNode.isObject() && toolResultNode.has("sql")) {
                    return parseSqlResponse(toolResultNode, mapper);
                }
            }
            
            return parseNestedContent(contentNode, mapper, originalResponse);
        }
        
        return null;
    }

    /**
     * 从响应中提取错误消息
     */
    private String extractErrorMessage(JsonNode root) {
        if (root.has("error")) {
            JsonNode errorNode = root.get("error");
            if (errorNode.isTextual()) {
                return errorNode.asText();
            } else if (errorNode.isObject() && errorNode.has("message")) {
                return errorNode.get("message").asText();
            }
        }
        if (root.has("errorMessage")) {
            return root.get("errorMessage").asText();
        }
        if (root.has("message")) {
            return root.get("message").asText();
        }
        return "";
    }

    /**
     * 从文本中查找嵌入的JSON片段
     */
    private String findEmbeddedJson(String text) {
        int startIndex = text.indexOf("{");
        int endIndex = text.lastIndexOf("}");
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String embeddedJson = text.substring(startIndex, endIndex + 1);
            // 验证是否是有效的JSON
            try {
                new ObjectMapper().readTree(embeddedJson);
                log.info("找到嵌入的JSON片段，长度: {}", embeddedJson.length());
                return embeddedJson;
            } catch (Exception e) {
                log.debug("找到的JSON片段无效: {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 从JsonNode中提取文本内容
     */
    private String extractTextContent(JsonNode node) {
        if (node.has("content")) {
            JsonNode contentNode = node.get("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            }
            if (contentNode.has("text")) {
                return contentNode.get("text").asText();
            }
        }
        if (node.has("text")) {
            return node.get("text").asText();
        }
        return null;
    }

    /**
     * 清理响应内容中的干扰字符
     */
    private String cleanResponse(String response) {
        // 移除可能的代码块标记
        String cleaned = response.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("\\s*```", "");
        
        // 移除可能的BOM字符
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
        }
        
        // 移除首尾空白
        cleaned = cleaned.trim();
        
        return cleaned;
    }
}