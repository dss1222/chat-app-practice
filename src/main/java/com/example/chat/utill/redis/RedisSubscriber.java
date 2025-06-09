package com.example.chat.utill.redis;

import com.example.chat.handler.WebSocketHandler;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class RedisSubscriber implements MessageListener {

    private final WebSocketHandler webSocketHandler;

    public RedisSubscriber(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channelMessage = new String(message.getBody(), StandardCharsets.UTF_8);

        Set<WebSocketSession> sessions = webSocketHandler.getSessions();
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(channelMessage));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
