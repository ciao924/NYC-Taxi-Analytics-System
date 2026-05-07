package com.taxi.analytics.modules.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.analytics.modules.ai.dsl.Dsl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParser.class);
    
    private final ObjectMapper objectMapper;

    public AiResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 从Coze响应中提取DSL
     * @param response Coze API原始响应
     * @return 解析后的Dsl对象
     */
    public Dsl parse(String response) {
        log.info("开始解析Coze响应，响应长度：{}", response != null ? response.length() : 0);
        
        try {
            // 1. 处理SSE格式的响应
            String jsonResponse = extractJsonFromSse(response);
            log.info("提取到JSON响应，长度：{}", jsonResponse.length());
            
            // 2. 解析JSON响应
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            // 3. 提取content字段
            if (!rootNode.has("content")) {
                throw new RuntimeException("Coze响应缺少content字段");
            }
            
            JsonNode contentNode = rootNode.get("content");
            
            // 4. 提取answer字段
            if (!contentNode.has("answer")) {
                throw new RuntimeException("Coze响应缺少answer字段");
            }
            
            JsonNode answerNode = contentNode.get("answer");
            String answer = answerNode.asText();
            
            // 5. 检查answer是否为空
            if (answer == null || answer.isEmpty()) {
                throw new RuntimeException("Coze响应的answer字段为空");
            }
            
            log.info("提取到answer字段，长度：{}", answer.length());
            
            // 6. 将answer字符串解析为Dsl对象
            Dsl dsl = objectMapper.readValue(answer, Dsl.class);
            log.info("DSL解析成功：{}", dsl);
            
            return dsl;
        } catch (Exception e) {
            log.error("解析Coze响应失败", e);
            throw new RuntimeException("解析Coze响应失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 从SSE格式响应中提取JSON数据
     * @param sseResponse SSE格式的响应
     * @return 提取的JSON字符串
     */
    private String extractJsonFromSse(String sseResponse) {
        log.info("开始从SSE响应中提取JSON数据");
        
        if (sseResponse == null || sseResponse.isEmpty()) {
            throw new RuntimeException("SSE响应为空");
        }
        
        // 分割SSE事件
        String[] lines = sseResponse.split("\n");
        StringBuilder jsonBuilder = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            // 提取data字段的内容
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if (!data.isEmpty() && !data.equals("[DONE]")) {
                    jsonBuilder.append(data);
                }
            }
        }
        
        String jsonResponse = jsonBuilder.toString();
        if (jsonResponse.isEmpty()) {
            throw new RuntimeException("SSE响应中未找到有效数据");
        }
        
        log.info("提取到JSON数据，长度：{}", jsonResponse.length());
        return jsonResponse;
    }
}