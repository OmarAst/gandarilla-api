package com.riogandarilla.api.security;

import com.riogandarilla.api.configs.properties.AppProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class DefaultTokenValidationService implements TokenValidationService {

    private final AppProperties properties;

    public DefaultTokenValidationService(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String token) {
        String expected = properties.apiBearerToken();
        if (expected.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
        );
    }
}
