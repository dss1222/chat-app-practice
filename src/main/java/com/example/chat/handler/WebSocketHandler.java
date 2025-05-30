package com.example.chat.handler;

import com.example.chat.dto.ChatMessage;
import com.example.chat.service.ChatMessageService;
import com.example.chat.utill.auth.JwtTokenProvider;
import com.example.chat.utill.constant.ChatConstants;
import com.example.chat.utill.redis.RedisPublisher;
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
    private final ChatMessageService chatMessageService;
    private final RedisPublisher redisPublisher;

    private static final String CHANNEL = "chat"; // Redis 채널명

    public WebSocketHandler(JwtTokenProvider jwtTokenProvider, ChatMessageService chatMessageService, RedisPublisher redisPublisher) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.chatMessageService = chatMessageService;
        this.redisPublisher = redisPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("New session connected (미인증 상태): " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("받은 메시지: " + payload);

        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        if (!authenticatedSessions.contains(session.getId())) {
            handleUnauthenticatedMessage(session, chatMessage);
            return;
        }

        handleAuthenticatedMessage(session, chatMessage);
    }

    private void handleUnauthenticatedMessage(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        if (chatMessage.type() != ChatMessage.MessageType.ENTER) {
            System.out.println("❌ 인증 안 된 세션의 메시지. 연결 종료: " + session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }

        String token = chatMessage.message();
        if (!validateAndAuthenticateToken(session, token)) {
            return;
        }

        String username = jwtTokenProvider.getUsername(token);
        sessionNicknames.put(session.getId(), username);
        authenticatedSessions.add(session.getId());

        System.out.println("✅ 인증 및 입장 성공: " + session.getId() + " 사용자: " + username);

        // ✅ 입장 메시지 Redis에 발행
        String enterMessage = chatMessageService.formatEnterMessage(username);
        redisPublisher.publish(CHANNEL, enterMessage);

        // ✅ 유저 목록도 Redis에 발행
        broadcastUserList();
    }

    private boolean validateAndAuthenticateToken(WebSocketSession session, String token) throws IOException {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            System.out.println("❌ 토큰 검증 실패. 연결 종료: " + session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid Token"));
            return false;
        }
        return true;
    }

    private void handleAuthenticatedMessage(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        String sender = sessionNicknames.get(session.getId());
        String broadcastMessage = chatMessageService.processMessage(chatMessage, sender);

        if (chatMessage.type() == ChatMessage.MessageType.LEAVE) {
            sessionNicknames.remove(session.getId());
            authenticatedSessions.remove(session.getId());
            broadcastUserList();
        }

        // ✅ Redis로 발행
        redisPublisher.publish(CHANNEL, broadcastMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        authenticatedSessions.remove(session.getId());
        String nickname = sessionNicknames.remove(session.getId());
        if (nickname != null) {
            String disconnectMessage = chatMessageService.formatDisconnectMessage(nickname);
            redisPublisher.publish(CHANNEL, disconnectMessage);
            broadcastUserList();
        }
        System.out.println("Session disconnected: " + session.getId());
    }

    private void broadcastUserList() throws IOException {
        List<String> nicknames = new ArrayList<>(sessionNicknames.values());
        Map<String, Object> userListPayload = Map.of(
                "type", ChatConstants.USER_LIST_TYPE,
                "users", nicknames
        );
        String json = objectMapper.writeValueAsString(userListPayload);
        redisPublisher.publish(CHANNEL, json);
        System.out.println("[유저 목록] " + nicknames);
    }

    // 세션 접근을 위해 (RedisSubscriber에서 사용)
    public Set<WebSocketSession> getSessions() {
        return sessions;
    }
}
