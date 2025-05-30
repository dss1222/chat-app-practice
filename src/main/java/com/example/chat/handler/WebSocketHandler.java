package com.example.chat.handler;

import com.example.chat.dto.ChatMessage;
import com.example.chat.auth.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, String> sessionNicknames = new ConcurrentHashMap<>();
    private final Set<String> authenticatedSessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketHandler(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 연결만 하고, 인증은 ENTER에서 진행
        sessions.add(session);
        System.out.println("New session connected (미인증 상태): " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("받은 메시지: " + payload);

        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        // 아직 인증 안 된 세션은 ENTER에서 인증
        if (!authenticatedSessions.contains(session.getId())) {
            if (chatMessage.type() == ChatMessage.MessageType.ENTER) {
                String token = chatMessage.message(); // ENTER 타입일 때 message에 토큰이 들어옴

                if (token == null || !jwtTokenProvider.validateToken(token)) {
                    System.out.println("❌ 토큰 검증 실패. 연결 종료: " + session.getId());
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid Token"));
                    return;
                }

                String username = jwtTokenProvider.getUsername(token);
                sessionNicknames.put(session.getId(), username);
                authenticatedSessions.add(session.getId());

                System.out.println("✅ 인증 및 입장 성공: " + session.getId() + " 사용자: " + username);

                String broadcastMessage = "🔵 [" + username + "] 님이 입장하셨습니다.";
                for (WebSocketSession ws : sessions) {
                    if (ws.isOpen()) {
                        ws.sendMessage(new TextMessage(broadcastMessage));
                    }
                }
                broadcastUserList();
                return; // ENTER 메시지는 추가 브로드캐스트 X
            } else {
                // 인증 안 된 세션에서 다른 메시지 보내면 끊기
                System.out.println("❌ 인증 안 된 세션의 메시지. 연결 종료: " + session.getId());
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
                return;
            }
        }

        // 인증된 세션만 여기 도달
        String sender = sessionNicknames.get(session.getId());
        String broadcastMessage;

        switch (chatMessage.type()) {
            case CHAT -> {
                broadcastMessage = "💬 [" + sender + "]: " + chatMessage.message();
            }
            case LEAVE -> {
                broadcastMessage = "🔴 [" + sender + "] 님이 퇴장하셨습니다.";
                sessionNicknames.remove(session.getId());
                authenticatedSessions.remove(session.getId());
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
        authenticatedSessions.remove(session.getId());
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
