package com.example.chat.handler;

import com.example.chat.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, String> sessionNicknames = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("New session connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // JSON 문자열 → ChatMessage 객체로 변환

        ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);

        String sender = chatMessage.sender();
        String broadcastMessage;

        System.out.println("파싱된 메시지 타입: " + chatMessage.type());


        switch (chatMessage.type()) {
            case ENTER -> {
                sessionNicknames.put(session.getId(), sender);
                broadcastMessage = "🔵 [" + sender + "] 님이 입장하셨습니다.";
                broadcastUserList();
            }
            case CHAT -> {
                broadcastMessage = "💬 [" + sender + "]: " + chatMessage.message();
            }
            case LEAVE -> {
                broadcastMessage = "🔴 [" + sender + "] 님이 퇴장하셨습니다.";
                sessionNicknames.remove(session.getId());
                broadcastUserList();
            }
            default -> {
                broadcastMessage = "[알 수 없는 메시지]";
            }
        }

        // 모든 세션에 메시지 브로드캐스트
        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                ws.sendMessage(new TextMessage(broadcastMessage));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        String nickname = sessionNicknames.remove(session.getId());
        if (nickname != null) {
            String leaveMessage = "🔴 [" + nickname + "] 님의 연결이 종료되었습니다.";
            for (WebSocketSession ws : sessions) {
                if (ws.isOpen()) {
                    ws.sendMessage(new TextMessage(leaveMessage));
                }
            }
            broadcastUserList();
        }
        System.out.println("Session disconnected: " + session.getId());
    }

    private void broadcastUserList() throws IOException {
        List<String> nicknames = new ArrayList<>(sessionNicknames.values());
        Map<String, Object> userListPayload = Map.of(
            "type", "USER_LIST",
            "users", nicknames
        );
        String json = objectMapper.writeValueAsString(userListPayload);
        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                ws.sendMessage(new TextMessage(json));
            }
        }
        System.out.println("[유저 목록] " + nicknames);
    }
    
}
