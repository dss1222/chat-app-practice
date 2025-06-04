package com.example.sdk;

import java.util.List;

public class ChatMessageFactory {
    private ChatMessageFactory() {
    } // 인스턴스화 방지

    public static ChatMessage createEnterMessage(String sender, String token) {
        return new ChatMessage(MessageType.ENTER.name(), sender, token, null);
    }

    public static ChatMessage createChatMessage(String sender, String message) {
        return new ChatMessage(MessageType.CHAT.name(), sender, message, null);
    }

    public static ChatMessage createLeaveMessage(String sender) {
        return new ChatMessage(MessageType.LEAVE.name(), sender, null, null);
    }

    public static ChatMessage createUserListMessage(String sender, List<String> users) {
        return new ChatMessage(MessageType.CHAT.name(), sender, null, users);
    }
}