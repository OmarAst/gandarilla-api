package com.riogandarilla.api.security;

public interface TokenValidationService {
    boolean isValid(String token);

    String generateToken(String subject);
}
