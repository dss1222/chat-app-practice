package com.example.sdk;

public class ChatClient {

    private final WebSocketConnectionManager connectionManager;
    private final TokenManager tokenManager;
    private final String nickname;
    private ChatListener listener;

    public ChatClient(String websocketUrl, String tokenApiUrl, String nickname) {
        this.connectionManager = new WebSocketConnectionManager(websocketUrl);
        this.tokenManager = new TokenManager(tokenApiUrl);
        this.nickname = nickname;
    }

    public void setListener(ChatListener listener) {
        this.listener = listener;
        this.connectionManager.setMessageHandler(this::handleMessage);
    }

    public void connect() {
        try {
            String token = tokenManager.requestToken(nickname);
            connectionManager.connect();

            // 연결되자마자 인증 및 입장 메시지
            ChatMessage enterMessage = ChatMessageFactory.createEnterMessage(nickname, token);
            connectionManager.sendMessage(enterMessage);
        } catch (Exception e) {
            throw new RuntimeException("채팅 연결 실패", e);
        }
    }

    public void sendMessage(String text) {
        if (!connectionManager.isConnected()) {
            throw new IllegalStateException("채팅이 연결되어 있지 않습니다.");
        }

        ChatMessage chatMessage = ChatMessageFactory.createChatMessage(nickname, text);
        connectionManager.sendMessage(chatMessage);
    }

    public void disconnect() {
        if (connectionManager.isConnected()) {
            ChatMessage leaveMessage = ChatMessageFactory.createLeaveMessage(nickname);
            connectionManager.sendMessage(leaveMessage);
        }
        connectionManager.disconnect();
    }

    private void handleMessage(ChatMessage message) {
        if (listener != null) {
            listener.onMessage(message);
        }
    }
}
