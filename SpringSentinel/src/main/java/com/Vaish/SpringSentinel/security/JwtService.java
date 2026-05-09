package com.Vaish.SpringSentinel.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey signingKey;

    // ============================================================
    // INITIALIZE SECRET KEY
    // ============================================================

    @PostConstruct
    public void init() {

        // ── Fail fast if secret is too short ─────────────────
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters! " +
                            "Current length: " +
                            (secret == null ? 0 : secret.length())
            );
        }

        signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ============================================================
    // GENERATE TOKEN
    // ============================================================

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis() + expiration
                ))
                .signWith(signingKey)
                .compact();
    }

    // ============================================================
    // EXTRACT USERNAME
    // ============================================================

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // ============================================================
    // VALIDATE TOKEN
    // ============================================================

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}