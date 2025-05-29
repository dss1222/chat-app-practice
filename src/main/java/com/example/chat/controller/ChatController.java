
package com.example.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    @GetMapping("/chat")
    public String chatPage() {
        return "chat"; // templates/chat.html 렌더링
    }
}
