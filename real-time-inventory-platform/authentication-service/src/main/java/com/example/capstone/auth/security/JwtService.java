package com.example.capstone.auth.security;

import com.example.capstone.auth.user.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String secret;
    private final Duration ttl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-minutes}") long ttlMinutes
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.secret = secret;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String createToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId() == null ? null : user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("displayName", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.get("userId", String.class);
        return new JwtPrincipal(
                userId == null ? null : UUID.fromString(userId),
                claims.getSubject(),
                claims.get("role", String.class)
        );
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
