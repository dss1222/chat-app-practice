package com.example.chat.handler;

import com.example.chat.dto.ChatMessage;
import com.example.chat.exception.AuthenticationException;
import com.example.chat.exception.MessageProcessingException;
import com.example.chat.service.ChatMessageService;
import com.example.chat.utill.auth.JwtTokenProvider;
import com.example.chat.utill.constant.ChatConstants;
import com.example.chat.utill.redis.RedisPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final String CHANNEL = "chat";
    private static final String ERROR_MESSAGE_PROCESSING = "메시지 처리 중 오류 발생";
    private static final String ERROR_CONNECTION_CLOSED = "연결 종료 처리 중 오류 발생";
    private static final String ERROR_USER_LIST_BROADCAST = "사용자 목록 브로드캐스트 중 오류 발생";

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, String> sessionNicknames = new ConcurrentHashMap<>();
    private final Set<String> authenticatedSessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenProvider jwtTokenProvider;
    private final ChatMessageService chatMessageService;
    private final RedisPublisher redisPublisher;

    public WebSocketHandler(JwtTokenProvider jwtTokenProvider, ChatMessageService chatMessageService,
            RedisPublisher redisPublisher) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.chatMessageService = chatMessageService;
        this.redisPublisher = redisPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatMessage chatMessage = parseMessage(message.getPayload());
            processMessage(session, chatMessage);
        } catch (Exception e) {
            log.error(ERROR_MESSAGE_PROCESSING, e);
            throw new MessageProcessingException(ERROR_MESSAGE_PROCESSING, e);
        }
    }

    private ChatMessage parseMessage(String payload) throws IOException {
        return objectMapper.readValue(payload, ChatMessage.class);
    }

    private void processMessage(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        if (!authenticatedSessions.contains(session.getId())) {
            handleUnauthenticatedMessage(session, chatMessage);
            return;
        }
        handleAuthenticatedMessage(session, chatMessage);
    }

    private void handleUnauthenticatedMessage(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        validateEnterMessage(chatMessage);
        String token = chatMessage.message();

        if (!validateAndAuthenticateToken(session, token)) {
            return;
        }

        authenticateSession(session, token);
    }

    private void validateEnterMessage(ChatMessage chatMessage) {
        if (chatMessage.type() != ChatMessage.MessageType.ENTER) {
            throw new AuthenticationException("인증되지 않은 세션의 메시지");
        }
    }

    private void authenticateSession(WebSocketSession session, String token) throws IOException {
        String username = jwtTokenProvider.getUsername(token);
        sessionNicknames.put(session.getId(), username);
        authenticatedSessions.add(session.getId());

        String enterMessage = chatMessageService.formatEnterMessage(username);
        redisPublisher.publish(CHANNEL, enterMessage);

        broadcastUserList();
    }

    private boolean validateAndAuthenticateToken(WebSocketSession session, String token) throws IOException {
        try {
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                throw new AuthenticationException("유효하지 않은 토큰");
            }
            return true;
        } catch (AuthenticationException e) {
            log.warn("❌ 토큰 검증 실패. 연결 종료: {} - {}", session.getId(), e.getMessage());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(e.getMessage()));
            return false;
        }
    }

    private void handleAuthenticatedMessage(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        String sender = getAuthenticatedSender(session);
        String broadcastMessage = chatMessageService.processMessage(chatMessage, sender);

        if (chatMessage.type() == ChatMessage.MessageType.LEAVE) {
            handleUserLeave(session);
        }

        redisPublisher.publish(CHANNEL, broadcastMessage);
    }

    private String getAuthenticatedSender(WebSocketSession session) {
        String sender = sessionNicknames.get(session.getId());
        if (sender == null) {
            throw new AuthenticationException("인증된 세션이지만 발신자 정보가 없음");
        }
        return sender;
    }

    private void handleUserLeave(WebSocketSession session) {
        sessionNicknames.remove(session.getId());
        authenticatedSessions.remove(session.getId());
        try {
            broadcastUserList();
        } catch (IOException e) {
            log.error(ERROR_USER_LIST_BROADCAST, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            handleSessionClose(session);
        } catch (Exception e) {
            log.error(ERROR_CONNECTION_CLOSED, e);
            throw new MessageProcessingException(ERROR_CONNECTION_CLOSED, e);
        }
    }

    private void handleSessionClose(WebSocketSession session) throws IOException {
        sessions.remove(session);
        authenticatedSessions.remove(session.getId());
        String nickname = sessionNicknames.remove(session.getId());

        if (nickname != null) {
            String disconnectMessage = chatMessageService.formatDisconnectMessage(nickname);
            redisPublisher.publish(CHANNEL, disconnectMessage);
            broadcastUserList();
        }
    }

    private void broadcastUserList() throws IOException {
        try {
            List<String> nicknames = new ArrayList<>(sessionNicknames.values());
            Map<String, Object> userListPayload = Map.of(
                    "type", ChatConstants.USER_LIST_TYPE,
                    "users", nicknames);
            String json = objectMapper.writeValueAsString(userListPayload);
            redisPublisher.publish(CHANNEL, json);
        } catch (Exception e) {
            log.error(ERROR_USER_LIST_BROADCAST, e);
            throw new MessageProcessingException(ERROR_USER_LIST_BROADCAST, e);
        }
    }

    public Set<WebSocketSession> getSessions() {
        return sessions;
    }
}
