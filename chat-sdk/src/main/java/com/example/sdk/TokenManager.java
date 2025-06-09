package com.example.sdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TokenManager {
    private final String tokenApiUrl;

    public TokenManager(String tokenApiUrl) {
        this.tokenApiUrl = tokenApiUrl;
    }

    public String requestToken(String username) {
        try {
            String apiUrl = tokenApiUrl + "?username=" + username;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("토큰 발급 실패. 응답 코드: " + responseCode);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return br.readLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("토큰 요청 실패", e);
        }
    }
}