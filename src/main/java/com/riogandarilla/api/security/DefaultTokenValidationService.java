package com.riogandarilla.api.security;

import com.riogandarilla.api.configs.properties.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class DefaultTokenValidationService implements TokenValidationService {

    private final AppProperties properties;

    public DefaultTokenValidationService(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String token) {
        if (properties.apiBearerSecret().isBlank() || token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = parser().parseSignedClaims(token).getPayload();
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public String generateToken(String subject) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(properties.apiBearerExpiration())))
                .signWith(signingKey())
                .compact();
    }

    private io.jsonwebtoken.JwtParser parser() {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.apiBearerSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
