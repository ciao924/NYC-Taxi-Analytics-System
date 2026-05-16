package com.taxi.analytics.modules.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class CozeClient {

    private static final Logger log = LoggerFactory.getLogger(CozeClient.class);

    private final ObjectMapper objectMapper;
    private final String cozeApiUrl;
    private final String cozeToken;
    private final String sessionId;
    private final String projectId;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public CozeClient(
            @Value("${ai.coze.api-url:https://k376w6hmfv.coze.site/stream_run}") String cozeApiUrl,
            @Value("${ai.coze.token:}") String cozeToken,
            @Value("${ai.coze.session-id:pP4Jc23cxHz7OLXOIoBZg}") String sessionId,
            @Value("${ai.coze.project-id:7631386060257230888}") String projectId,
            @Value("${ai.coze.connect-timeout:10000}") int connectTimeoutMs,
            @Value("${ai.coze.timeout:60000}") int readTimeoutMs) {
        this.objectMapper = new ObjectMapper();
        this.cozeApiUrl = cozeApiUrl;
        this.cozeToken = cozeToken;
        this.sessionId = sessionId;
        this.projectId = projectId;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        log.info("CozeClient 初始化完成 - API地址: {}, sessionId: {}, projectId: {}, connectTimeout: {}ms, readTimeout: {}ms", 
                cozeApiUrl, sessionId, projectId, connectTimeoutMs, readTimeoutMs);
    }

    public String callAI(String question) {
        return callAIWithSchema(question, null);
    }

    public String callAIWithSchema(String question, String schemaJson) {
        log.info("调用 Coze API，问题：{}，是否携带schema：{}", question, schemaJson != null ? "是" : "否");

        HttpURLConnection connection = null;
        try {
            // 验证token是否配置
            if (cozeToken == null || cozeToken.isEmpty()) {
                log.error("Coze API Token 未配置，请在 application.yml 中设置 ai.coze.token");
                throw new RuntimeException("Coze API Token 未配置");
            }

            CozeRequest request = new CozeRequest(question, schemaJson);
            String requestBody = objectMapper.writeValueAsString(request);

            log.debug("Coze API 请求体: {}", requestBody);

            URL url = new URL(cozeApiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + cozeToken);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            log.info("Coze API 响应状态码: {}", responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorResponse = readErrorResponse(connection);
                log.error("Coze API 调用失败，状态码：{}，错误响应：{}", responseCode, errorResponse);
                throw new RuntimeException("Coze API 调用失败，状态码：" + responseCode + "，错误：" + errorResponse);
            }

            InputStream inputStream = connection.getInputStream();
            String responseBody = readSseStream(inputStream);
            log.info("Coze API 响应成功，响应长度：{}", responseBody != null ? responseBody.length() : 0);

            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Coze API 返回空响应");
                return "";
            }

            String result = parseSseResponse(responseBody);
            log.info("解析后响应：{}", result);

            return result;

        } catch (IOException e) {
            log.error("调用 Coze API IO异常: {}", e.getMessage(), e);
            throw new RuntimeException("调用 Coze API IO异常：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("调用 Coze API 异常: {}", e.getMessage(), e);
            throw new RuntimeException("调用 Coze API 异常：" + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readErrorResponse(HttpURLConnection connection) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            log.warn("读取错误响应失败: {}", e.getMessage());
        }
        return sb.toString();
    }

    private String readSseStream(InputStream inputStream) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        int lineCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\n");
                lineCount++;

                if (lineCount % 100 == 0) {
                    log.debug("已读取 {} 行", lineCount);
                }

                if (line.contains("\"type\":\"message_end\"")) {
                    log.debug("收到 message_end，停止读取");
                    break;
                }
            }
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("stream is closed")) {
                log.warn("SSE流在读取过程中被关闭，已读取 {} 行", lineCount);
                if (responseBuilder.length() > 0) {
                    return responseBuilder.toString();
                }
            }
            throw e;
        }

        log.info("SSE流读取完成，共 {} 行", lineCount);
        return responseBuilder.toString();
    }

    private String parseSseResponse(String sseResponse) {
        if (sseResponse == null || sseResponse.trim().isEmpty()) {
            log.warn("SSE响应为空");
            return "";
        }

        StringBuilder answerBuilder = new StringBuilder();
        int answerCount = 0;
        int contentCount = 0;
        int toolResultCount = 0;
        int messageStartCount = 0;
        int unknownCount = 0;
        boolean isComplete = false;

        String[] lines = sseResponse.split("\\r?\\n");

        log.debug("SSE响应共 {} 行", lines.length);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) continue;

            if (line.startsWith("data:")) {
                String dataContent = line.substring(5).trim();
                if (dataContent.isEmpty()) continue;

                try {
                    JsonNode jsonNode = objectMapper.readTree(dataContent);
                    String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

                    log.debug("处理第 {} 行，类型: {}", i + 1, type);

                    // ====== 终止事件 ======
                    if ("message_end".equals(type)) {
                        log.debug("收到 message_end 事件，停止解析");
                        isComplete = true;
                        break;
                    }

                    // ====== 消息开始事件 ======
                    // 注意：message_start 事件可能带有 finish: true，但这只是表示消息开始事件的结束
                    // 不是整个响应的结束，所以不能在这里 break
                    if ("message_start".equals(type)) {
                        messageStartCount++;
                        log.debug("收到 message_start 事件 (#{})", messageStartCount);
                        // 继续处理后续事件，即使有 finish: true 也不能 break
                        continue;
                    }

                    // ====== finish 标记检查 ======
                    // 只有在非 message_start 事件时，finish: true 才表示响应结束
                    if (jsonNode.has("finish") && jsonNode.get("finish").asBoolean()) {
                        log.debug("收到 finish 标记（类型: {}），停止解析", type);
                        isComplete = true;
                        break;
                    }

                    // ====== Answer 事件（智能体增量返回的主要方式） ======
                    if ("answer".equals(type)) {
                        answerCount++;
                        String answerPart = extractTextFromNode(jsonNode);
                        if (answerPart != null && !answerPart.isEmpty()) {
                            answerBuilder.append(answerPart);
                            log.debug("收集到 answer 片段 #{}，长度: {}, 累积长度: {}", 
                                answerCount, answerPart.length(), answerBuilder.length());
                        }
                    }
                    // ====== Content 事件 ======
                    else if ("content".equals(type)) {
                        contentCount++;
                        if (jsonNode.has("content")) {
                            JsonNode contentNode = jsonNode.get("content");
                            String textContent = extractTextFromNode(contentNode);
                            
                            if (textContent != null) {
                                // 如果是完整的JSON对象，直接返回
                                if (textContent.trim().startsWith("{") && textContent.trim().endsWith("}")) {
                                    log.info("发现完整的 JSON content，长度: {}", textContent.length());
                                    return extractJsonWithBoundary(textContent);
                                }
                                answerBuilder.append(textContent);
                                log.debug("收集到 content 片段 #{}，长度: {}, 累积长度: {}", 
                                    contentCount, textContent.length(), answerBuilder.length());
                            }
                        }
                    }
                    // ====== Tool Result 事件 ======
                    else if ("tool_result".equals(type)) {
                        toolResultCount++;
                        String toolResult = extractToolResult(jsonNode);
                        if (toolResult != null && !toolResult.isEmpty()) {
                            answerBuilder.append(toolResult);
                            log.debug("收集到 tool_result 片段 #{}，长度: {}, 累积长度: {}", 
                                toolResultCount, toolResult.length(), answerBuilder.length());
                        }
                    }
                    // ====== Message 事件（通用消息类型） ======
                    else if ("message".equals(type)) {
                        contentCount++;
                        String messageContent = extractTextFromNode(jsonNode);
                        if (messageContent != null && !messageContent.isEmpty()) {
                            // 如果是完整的JSON对象，直接返回
                            if (messageContent.trim().startsWith("{") && messageContent.trim().endsWith("}")) {
                                log.info("发现完整的 JSON message，长度: {}", messageContent.length());
                                return extractJsonWithBoundary(messageContent);
                            }
                            answerBuilder.append(messageContent);
                            log.debug("收集到 message 片段 #{}，长度: {}, 累积长度: {}", 
                                contentCount, messageContent.length(), answerBuilder.length());
                        }
                    }
                    // ====== 未知类型事件 ======
                    else {
                        unknownCount++;
                        log.debug("收到未知类型事件: {}", type);
                        // 尝试提取文本内容
                        String unknownContent = extractTextFromNode(jsonNode);
                        if (unknownContent != null && !unknownContent.isEmpty()) {
                            answerBuilder.append(unknownContent);
                        }
                    }

                } catch (Exception e) {
                    log.warn("解析SSE行失败 (行 {}): {}, 错误: {}", i + 1, line.length() > 100 ? line.substring(0, 100) + "..." : line, e.getMessage());
                    // 将原始数据添加到answerBuilder，以防是纯文本响应
                    answerBuilder.append(dataContent);
                }
            }
            // 处理 event: 行
            else if (line.startsWith("event:")) {
                log.debug("事件类型: {}", line.substring(6).trim());
            }
            // 处理其他行
            else {
                log.debug("忽略非数据行: {}", line.length() > 50 ? line.substring(0, 50) + "..." : line);
            }
        }

        log.info("SSE解析完成 - answer: {}, content: {}, tool_result: {}, message_start: {}, unknown: {}, 累积总长度: {}, 是否完整: {}",
                answerCount, contentCount, toolResultCount, messageStartCount, unknownCount, answerBuilder.length(), isComplete);

        String allText = answerBuilder.toString();
        if (allText.isEmpty()) {
            log.warn("SSE解析结果为空");
            return "";
        }

        return extractJsonWithBoundary(allText);
    }

    /**
     * 从JsonNode中提取文本内容
     */
    private String extractTextFromNode(JsonNode node) {
        if (node == null) return null;

        // 直接是文本节点
        if (node.isTextual()) {
            return node.asText();
        }

        // 尝试从常见字段提取
        String[] textFields = {"text", "answer", "content", "message", "data"};
        for (String field : textFields) {
            if (node.has(field)) {
                JsonNode fieldNode = node.get(field);
                if (fieldNode.isTextual()) {
                    return fieldNode.asText();
                } else if (fieldNode.isObject()) {
                    // 递归提取
                    return extractTextFromNode(fieldNode);
                }
            }
        }

        return null;
    }

    /**
     * 从tool_result类型的响应中提取结果
     */
    private String extractToolResult(JsonNode jsonNode) {
        if (jsonNode.has("content")) {
            JsonNode contentNode = jsonNode.get("content");
            if (contentNode.has("tool_result")) {
                JsonNode toolResultNode = contentNode.get("tool_result");
                return extractTextFromNode(toolResultNode);
            }
            return extractTextFromNode(contentNode);
        }
        return extractTextFromNode(jsonNode);
    }

    private String extractJsonWithBoundary(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        final String START_MARKER = "<<<START>>>";
        final String END_MARKER = "<<<END>>>";

        int startIndex = text.indexOf(START_MARKER);
        int endIndex = text.indexOf(END_MARKER);

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String jsonContent = text.substring(startIndex + START_MARKER.length(), endIndex).trim();
            log.info("成功提取边界标记内的JSON，长度: {}", jsonContent.length());
            return jsonContent;
        }

        String trimmedText = text.trim();
        if (trimmedText.startsWith("{") && trimmedText.endsWith("}")) {
            log.info("未找到边界标记，但内容本身是完整的JSON");
            return trimmedText;
        }

        log.debug("未找到边界标记且不是完整JSON，返回原始文本");
        return text;
    }

    private class CozeRequest {
        private final Content content;
        private final String type = "query";
        private final String session_id;
        private final String project_id;

        public CozeRequest(String question) {
            this(question, null);
        }

        public CozeRequest(String question, String schemaJson) {
            this.content = new Content(question, schemaJson);
            this.session_id = CozeClient.this.sessionId;
            this.project_id = CozeClient.this.projectId;
        }

        public Content getContent() {
            return content;
        }

        public String getType() {
            return type;
        }

        public String getSession_id() {
            return session_id;
        }

        public String getProject_id() {
            return project_id;
        }

        private static class Content {
            private final Query query;

            public Content(String question, String schemaJson) {
                this.query = new Query(question, schemaJson);
            }

            public Query getQuery() {
                return query;
            }

            private static class Query {
                private final List<Prompt> prompt;

                public Query(String question, String schemaJson) {
                    String enhancedQuestion = enhanceQuestionWithSchema(question, schemaJson);
                    this.prompt = Collections.singletonList(new Prompt(enhancedQuestion));
                }

                private String enhanceQuestionWithSchema(String question, String schemaJson) {
                    if (schemaJson == null || schemaJson.isEmpty()) {
                        return question;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("你是一个专业的数据分析SQL生成助手。请仔细分析用户问题，根据以下数据库schema信息生成正确的SQL查询。\n\n");
                    sb.append("=== 数据库Schema信息 ===\n");
                    sb.append(schemaJson);
                    sb.append("\n\n");
                    sb.append("=== 分析规则 ===\n");
                    sb.append("1. 仔细理解用户问题的核心意图：\n");
                    sb.append("   - 如果涉及'供应商'、'vendor'、'市场份额'等关键词，必须使用 analysis_vendor 表，查询 vendor_name 维度\n");
                    sb.append("   - 如果涉及'订单趋势'、'时间变化'等关键词，使用 analysis_kpi_daily 表，查询 stat_date 维度\n");
                    sb.append("   - 如果涉及'支付方式'，使用 analysis_payment_analysis 表\n");
                    sb.append("   - 如果涉及'区域'、'行政区'、'热点'，使用相应的区域分析表\n");
                    sb.append("\n");
                    sb.append("2. SQL生成要求：\n");
                    sb.append("   - 必须使用schema中提供的表名和字段名，不得凭空捏造\n");
                    sb.append("   - SQL必须是有效的MySQL语法，支持GROUP BY、ORDER BY、LIMIT等\n");
                    sb.append("   - 使用SUM()函数计算总量指标，使用AVG()计算平均值\n");
                    sb.append("   - 查询结果字段名应与chart_config中的x_field、y_field对应\n");
                    sb.append("\n");
                    sb.append("3. 图表配置要求：\n");
                    sb.append("   - chart_type：时间趋势用'line'，分类对比用'bar'，占比分析用'pie'\n");
                    sb.append("   - x_field：维度字段名（如stat_date、vendor_name、payment_name）\n");
                    sb.append("   - y_field：指标字段名（如trip_count、total_revenue、revenue_ratio）\n");
                    sb.append("   - 注意：SQL查询结果的字段名必须与x_field、y_field完全匹配\n");
                    sb.append("\n");
                    sb.append("4. 返回格式要求（必须是有效的JSON）：\n");
                    sb.append("{\n");
                    sb.append("  \"sql\": \"SQL查询语句\",\n");
                    sb.append("  \"explanation\": \"业务解释文字\",\n");
                    sb.append("  \"chart_config\": {\n");
                    sb.append("    \"chart_type\": \"line|bar|pie|stacked_bar|horizontal_bar\",\n");
                    sb.append("    \"title\": \"图表标题\",\n");
                    sb.append("    \"x_field\": \"维度字段名\",\n");
                    sb.append("    \"y_field\": \"指标字段名\",\n");
                    sb.append("    \"legend\": true,\n");
                    sb.append("    \"percentage\": false\n");
                    sb.append("  }\n");
                    sb.append("}\n");
                    sb.append("\n");
                    sb.append("=== 用户问题 ===\n");
                    sb.append(question);

                    return sb.toString();
                }

                public List<Prompt> getPrompt() {
                    return prompt;
                }

                private static class Prompt {
                    private final String type = "text";
                    private final PromptContent content;

                    public Prompt(String question) {
                        this.content = new PromptContent(question);
                    }

                    public String getType() {
                        return type;
                    }

                    public PromptContent getContent() {
                        return content;
                    }

                    private static class PromptContent {
                        private final String text;

                        public PromptContent(String text) {
                            this.text = text;
                        }

                        public String getText() {
                            return text;
                        }
                    }
                }
            }
        }
    }
}