
package com.example.chat.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    // override 하기전 메서드는 Hook Point만 제공 -> 템플릿 메서드 패턴

    // 연결된 세션 관리
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    // 연결 성공 시
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("New session connected: " + session.getId());
    }

    // 메시지 보낼 시   
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 모든 세션에 메시지 전송
        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                ws.sendMessage(message);
            }
        }
    }

    // 연결 종료 시
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Session disconnected: " + session.getId());
    }
}
