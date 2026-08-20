package com.riogandarilla.api.security;

public interface TokenValidationService {
    boolean isValid(String token);
}
