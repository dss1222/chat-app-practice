package com.example.sdk.example;

import com.example.sdk.ChatClient;
import com.example.sdk.ChatListener;
import com.example.sdk.ChatMessage;

public class ExampleChatClient {
    public static void main(String[] args) {
        try {
            // WebSocket 서버 URL, Token 발급 API URL 설정
            String serverUrl = "ws://localhost:8080/ws/chat";
            String tokenApiUrl = "http://localhost:8080/api/token";

            // ChatClient 인스턴스 생성
            ChatClient chatClient = new ChatClient(serverUrl, tokenApiUrl);

            // 닉네임 설정
            chatClient.setNickname("example-user");

            // 메시지 리스너 설정
            chatClient.setListener(new ChatListener() {
                @Override
                public void onMessage(ChatMessage message) {
                    System.out.println("받은 메시지: " + message.getSender() + ": " + message.getMessage());
                }
            });

            // 서버에 연결 (내부적으로 token 발급 후 연결)
            chatClient.connect();

            // 예제 메시지 전송
            chatClient.sendMessage("Hello from example client!");

            // 프로그램이 종료되지 않도록 대기
            Thread.sleep(5000);

            // 연결 종료
            chatClient.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
