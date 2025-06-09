package com.example.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(
        String type,
        String sender,
        String message,
        List<String> users) {
}
