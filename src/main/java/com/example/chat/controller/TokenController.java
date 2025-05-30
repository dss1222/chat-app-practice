package com.example.chat.controller;

import org.springframework.web.bind.annotation.*;

import com.example.chat.utill.auth.JwtTokenProvider;

@RestController
@RequestMapping("/api")
public class TokenController {

    private final JwtTokenProvider jwtTokenProvider;

    public TokenController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/token")
    public String getToken(@RequestParam String username) {
        return jwtTokenProvider.createToken(username);
    }
}
