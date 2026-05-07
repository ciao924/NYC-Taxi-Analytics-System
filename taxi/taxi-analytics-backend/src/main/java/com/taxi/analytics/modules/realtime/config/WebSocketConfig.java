package com.taxi.analytics.modules.realtime.config;

import com.taxi.analytics.modules.realtime.handler.RealtimeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;

    public WebSocketConfig(RealtimeWebSocketHandler realtimeWebSocketHandler) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime").setAllowedOrigins("*");
    }
}
