package com.example.chat.utill.auth;

import com.example.chat.exception.AuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

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
        log.debug("Creating token for user: {}", username);
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
            log.debug("Token validation successful");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired");
            throw new AuthenticationException("Token has expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
            throw new AuthenticationException("Unsupported JWT token");
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token");
            throw new AuthenticationException("Invalid JWT token");
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature");
            throw new AuthenticationException("Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty");
            throw new AuthenticationException("JWT claims string is empty");
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
            log.error("Failed to get username from token", e);
            throw new AuthenticationException("Failed to get username from token", e);
        }
    }
}
