package com.example.chat.service;

import com.example.chat.dto.ChatMessage;
import com.example.chat.utill.constant.ChatConstants;

import org.springframework.stereotype.Service;

@Service
public class ChatMessageService {

    public String processMessage(ChatMessage chatMessage, String sender) {
        return switch (chatMessage.type()) {
            case CHAT -> formatChatMessage(sender, chatMessage.message());
            case LEAVE -> formatLeaveMessage(sender);
            default -> ChatConstants.UNKNOWN_MESSAGE;
        };
    }

    public String formatEnterMessage(String username) {
        return String.format(ChatConstants.ENTER_MESSAGE_FORMAT, username);
    }

    public String formatLeaveMessage(String username) {
        return String.format(ChatConstants.LEAVE_MESSAGE_FORMAT, username);
    }

    public String formatChatMessage(String sender, String message) {
        return String.format(ChatConstants.CHAT_MESSAGE_FORMAT, sender, message);
    }

    public String formatDisconnectMessage(String username) {
        return String.format(ChatConstants.DISCONNECT_MESSAGE_FORMAT, username);
    }
}