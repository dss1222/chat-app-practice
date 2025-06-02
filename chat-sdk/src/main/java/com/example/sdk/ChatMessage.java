package com.example.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    private String type;
    private String sender;
    private String message;
    private List<String> users; // 추가 ✅

    public ChatMessage() {}

    public ChatMessage(String type, String sender, String message) {
        this.type = type;
        this.sender = sender;
        this.message = message;
    }

    // getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getUsers() { return users; }   // getter 추가
    public void setUsers(List<String> users) { this.users = users; }   // setter 추가
}
