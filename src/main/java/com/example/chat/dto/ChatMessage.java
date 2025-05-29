
package com.example.chat.dto;

public record ChatMessage(
    MessageType type,
    String sender,
    String message
) {
    public enum MessageType {
        ENTER, CHAT, LEAVE
    }
}
