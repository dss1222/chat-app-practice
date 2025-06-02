package com.example.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class ChatClient {

    private final String websocketUrl;
    private final String tokenApiUrl;
    private WebSocketClient socketClient;
    private ChatListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String nickname;
    private String token;

    public ChatClient(String websocketUrl, String tokenApiUrl) {
        this.websocketUrl = websocketUrl;
        this.tokenApiUrl = tokenApiUrl;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setListener(ChatListener listener) {
        this.listener = listener;
    }

    public void connect() {
        try {
            this.token = requestTokenFromServer(nickname);

            socketClient = new WebSocketClient(new URI(websocketUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ WebSocket 연결됨");

                    // 연결되자마자 인증 및 입장 메시지
                    ChatMessage enterMessage = new ChatMessage("ENTER", nickname, token);
                    sendMessageInternal(enterMessage);
                }

                @Override
                public void onMessage(String message) {
                    try {
                        if (message.startsWith("{")) {  // 간단한 JSON 시작 포맷 체크
                            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
                            if (listener != null) {
                                listener.onMessage(chatMessage);
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

    public void sendMessage(String text) {
        ChatMessage chatMessage = new ChatMessage("CHAT", nickname, text);
        sendMessageInternal(chatMessage);
    }

    private void sendMessageInternal(ChatMessage chatMessage) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(chatMessage);
            socketClient.send(jsonMessage);
        } catch (Exception e) {
            System.err.println("메시지 전송 실패: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (socketClient != null) {
            socketClient.close();
        }
    }

    // ➡️ Token 발급 서버 API 호출
    private String requestTokenFromServer(String nickname) throws Exception {
        String apiUrl = tokenApiUrl + "?username=" + nickname;

        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setDoOutput(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("토큰 발급 실패. 응답 코드: " + responseCode);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return br.readLine();  // JWT 토큰 문자열을 그대로 리턴
        }
    }
}
