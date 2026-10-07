package com.phonebook.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.phonebook.model.LoginResponse;
import com.phonebook.model.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@Service
public class JwtService {

    private static final long TOKEN_LIFETIME_HOURS = 1;

    private final String issuer;
    private final String audience;
    private final SecretKey signingKey;

    public JwtService(
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience,
            @Value("${jwt.key}") String key) {

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                "jwt.key must contain at least 32 bytes of key material."
            );
        }

        this.issuer = issuer;
        this.audience = audience;
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public LoginResponse createToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(TOKEN_LIFETIME_HOURS * 60 * 60);

        String token = Jwts.builder()
            .issuer(issuer)
            .audience()
                .add(audience)
                .and()
            .claim("nameid", Integer.toString(user.getId()))
            .claim("unique_name", user.getUsername())
            .notBefore(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();

        LocalDateTime expirationDateTime =
            LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);

        return new LoginResponse(
            token,
            user.getUsername(),
            expirationDateTime
        );
    }
    
    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
}