package com.example.chat.utill.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:MySuperSecretKeyMySuperSecretKey1234}")
    private String secretKeyString;

    @Value("${jwt.expiration:3600000}")
    private long validityInMilliseconds;

    private Key secretKey;

    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    public String createToken(String username) {
        Claims claims = Jwts.claims().setSubject(username);

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("Token has expired");
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("Unsupported JWT token");
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("Invalid JWT token");
        } catch (SignatureException e) {
            throw new JwtAuthenticationException("Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT claims string is empty");
        }
    }

    public String getUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Failed to get username from token");
        }
    }
}

class JwtAuthenticationException extends RuntimeException {
    public JwtAuthenticationException(String message) {
        super(message);
    }
}
