package com.taxi.analytics.modules.ai.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * 自定义HttpMessageConverter，用于处理SSE（Server-Sent Events）流式响应
 * 
 * 核心问题：RestTemplate默认不支持text/event-stream内容类型返回InputStream
 * 错误信息：Could not extract response: no suitable HttpMessageConverter found 
 *          for response type [class java.io.InputStream] and content type [text/event-stream;charset=utf-8]
 * 
 * 修复策略：
 * 1. 继承AbstractHttpMessageConverter获得更好的兼容性
 * 2. 明确支持text/event-stream内容类型
 * 3. 直接返回输入流供调用者处理
 */
public class SseStreamHttpMessageConverter extends AbstractHttpMessageConverter<InputStream> {

    private static final Logger log = LoggerFactory.getLogger(SseStreamHttpMessageConverter.class);

    public SseStreamHttpMessageConverter() {
        super(MediaType.TEXT_EVENT_STREAM);
        log.info("SseStreamHttpMessageConverter 初始化完成");
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return InputStream.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        // 支持InputStream及其子类，且内容类型为text/event-stream
        boolean supportsClazz = InputStream.class.isAssignableFrom(clazz);
        boolean supportsMediaType = (mediaType == null) 
            || MediaType.TEXT_EVENT_STREAM.includes(mediaType)
            || (mediaType.getType() != null && mediaType.getType().equals("text"))
            || (mediaType.getSubtype() != null && mediaType.getSubtype().contains("event-stream"))
            || mediaType.toString().toLowerCase().contains("text/event-stream");
        
        log.debug("canRead - clazz: {}, mediaType: {}, supportsClazz: {}, supportsMediaType: {}", 
            clazz, mediaType, supportsClazz, supportsMediaType);
        
        return supportsClazz && supportsMediaType;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
    }

    @Override
    protected InputStream readInternal(Class<? extends InputStream> clazz, HttpInputMessage inputMessage) 
            throws IOException, HttpMessageNotReadableException {
        log.debug("读取SSE流式响应，内容类型: {}", inputMessage.getHeaders().getContentType());
        return inputMessage.getBody();
    }

    @Override
    protected void writeInternal(InputStream t, HttpOutputMessage outputMessage) 
            throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException("Writing is not supported for SSE streams");
    }
}
