package com.example.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class WebSocketConnectionManager {
    private final String websocketUrl;
    private final ObjectMapper objectMapper;
    private WebSocketClient socketClient;
    private Consumer<ChatMessage> messageHandler;

    public WebSocketConnectionManager(String websocketUrl) {
        this.websocketUrl = websocketUrl;
        this.objectMapper = new ObjectMapper();
    }

    public void setMessageHandler(Consumer<ChatMessage> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void connect() {
        try {
            socketClient = new WebSocketClient(new URI(websocketUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ WebSocket 연결됨");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        if (message.startsWith("{")) {
                            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
                            if (messageHandler != null) {
                                messageHandler.accept(chatMessage);
                            }
                        } else {
                            System.out.println("서버로부터 일반 텍스트 메시지 수신: " + message);
                        }
                    } catch (Exception e) {
                        System.err.println("메시지 파싱 에러: " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("❌ WebSocket 연결 종료: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("🚨 WebSocket 에러: " + ex.getMessage());
                }
            };

            socketClient.connect();
        } catch (Exception e) {
            throw new RuntimeException("WebSocket 연결 실패", e);
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            if (socketClient != null && socketClient.isOpen()) {
                socketClient.send(jsonMessage);
            } else {
                throw new IllegalStateException("WebSocket이 연결되어 있지 않습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("메시지 전송 실패", e);
        }
    }

    public void disconnect() {
        if (socketClient != null) {
            socketClient.close();
        }
    }

    public boolean isConnected() {
        return socketClient != null && socketClient.isOpen();
    }
}