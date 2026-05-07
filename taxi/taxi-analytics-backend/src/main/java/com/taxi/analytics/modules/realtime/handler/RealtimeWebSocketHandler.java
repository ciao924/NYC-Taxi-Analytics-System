package com.taxi.analytics.modules.realtime.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.analytics.modules.realtime.service.RealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeService realtimeService;
    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    @Scheduled(fixedRate = 5000) // 每5秒推送一次
    public void pushRealtimeData() {
        try {
            Map<String, Object> kpiData = realtimeService.getLatestKpi();
            Map<String, Object> message = Map.of(
                    "type", "KPI",
                    "data", kpiData
            );
            String jsonMessage = objectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
