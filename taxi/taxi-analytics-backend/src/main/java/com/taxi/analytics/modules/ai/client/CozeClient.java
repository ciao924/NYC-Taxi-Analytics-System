package com.taxi.analytics.modules.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class CozeClient {

    private static final Logger log = LoggerFactory.getLogger(CozeClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String cozeApiUrl;

    public CozeClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.cozeApiUrl = "https://k376w6hmfv.coze.site/stream_run";
    }

    public String callAI(String question) {
        log.info("调用 Coze API，问题：{}", question);

        try {
            // 正确的 Coze API 令牌
            String token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjMxMWQwNzVjLTY2NDktNDdmYi04MWUxLTJmZDIyYTJmMTgxOSJ9.eyJpc3MiOiJodHRwczovL2FwaS5jb3plLmNuIiwiYXVkIjpbInh3YlVVSTBBMGdFdHJCUXZuN2h1RjZOSnBiVkpFN0VvIl0sImV4cCI6ODIxMDI2Njg3Njc5OSwiaWF0IjoxNzc2ODI5MjA4LCJzdWIiOiJzcGlmZmU6Ly9hcGkuY296ZS5jbi93b3JrbG9hZF9pZGVudGl0eS9pZDo3NjMxMzg5MjkwNDU0MzE5MTQ0Iiwic3JjIjoiaW5ib3VuZF9hdXRoX2FjY2Vzc190b2tlbl9pZDo3NjMxNDIzMzM5NzU5NjY1MTYyIn0.fqg8ck2wahFaTKb9lG1cASt-FY5SS-6xbcU17l46ANU9IAZcKXjPWmr8M653lq0_9TBjtg556Hc4d4uEzanYszArIkA3dGMHW6wYTsFiAIUGBaKGX3jCWeDHNgLgRedPkhBzGlLpvdvqZthwj-GYQftcFZ6ctj44a8jDL9DXeMmOWKxUVETl6PydlFzyLFJ6h2t6_irDWvpej7uTNk1TbOqWst91xmG-zLiQnbtiA61qXzlvz4MbjDhiKrWG2QhcuFMhkNsRb-CQVdZixeCeLnOhy_wIKPxmyfJtc8T8LPZhWgaUS0JtZzXRFiCb42dTinOAx6ocTCbzz6y5FyFQDg";
            
            // 构建符合 Coze API 要求的请求体
            CozeRequest request = new CozeRequest(question);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
            // 添加 Authorization 头
            headers.set("Authorization", "Bearer " + token);

            // 打印调试日志
            log.info("Coze API 请求头 Authorization: Bearer {}", token.substring(0, 20) + "...");
            log.info("Coze API 请求地址: {}", cozeApiUrl);
            log.info("Coze API 请求体: {}", objectMapper.writeValueAsString(request));

            HttpEntity<CozeRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    cozeApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                log.info("Coze API 响应成功，响应长度：{}", responseBody != null ? responseBody.length() : 0);
                return responseBody;
            } else {
                log.error("Coze API 调用失败，状态码：{}，响应：{}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Coze API 调用失败，状态码：" + response.getStatusCode() + "，响应：" + response.getBody());
            }
        } catch (Exception e) {
            log.error("调用 Coze API 异常", e);
            throw new RuntimeException("调用 Coze API 异常：" + e.getMessage(), e);
        }
    }

    private static class CozeRequest {
        private final Content content;
        private final String type = "query";
        private final String session_id = "pP4Jc23cxHz7OLXOIoBZg";
        private final String project_id = "7631386060257230888";

        public CozeRequest(String question) {
            this.content = new Content(question);
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

            public Content(String question) {
                this.query = new Query(question);
            }

            public Query getQuery() {
                return query;
            }

            private static class Query {
                private final List<Prompt> prompt;

                public Query(String question) {
                    this.prompt = Collections.singletonList(new Prompt(question));
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