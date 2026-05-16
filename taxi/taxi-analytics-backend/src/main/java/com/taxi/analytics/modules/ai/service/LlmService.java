package com.taxi.analytics.modules.ai.service;

import com.taxi.analytics.modules.ai.service.dto.LlmResponse;

/**
 * LLM服务接口
 * 负责调用智能体并解析响应
 */
public interface LlmService {

    /**
     * 调用智能体获取响应
     * 
     * @param query 用户自然语言查询
     * @return LlmResponse 统一响应格式
     */
    LlmResponse callAgent(String query);

    /**
     * 调用智能体获取响应（带上下文）
     * 
     * @param query 用户自然语言查询
     * @param sessionId 会话ID（用于多轮对话）
     * @return LlmResponse 统一响应格式
     */
    LlmResponse callAgent(String query, String sessionId);
}