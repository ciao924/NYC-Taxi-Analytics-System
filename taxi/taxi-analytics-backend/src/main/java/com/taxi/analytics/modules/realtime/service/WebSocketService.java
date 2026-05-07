package com.taxi.analytics.modules.realtime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final ObjectMapper objectMapper;

    public void sendMessage(WebSocketSession session, String type, Object data) throws IOException {
        Map<String, Object> message = Map.of(
                "type", type,
                "data", data
        );
        String jsonMessage = objectMapper.writeValueAsString(message);
        session.sendMessage(new org.springframework.web.socket.TextMessage(jsonMessage));
    }
}
