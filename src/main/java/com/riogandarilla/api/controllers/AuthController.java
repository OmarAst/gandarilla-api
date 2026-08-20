package com.riogandarilla.api.controllers;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.security.TokenValidationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenValidationService tokenValidationService;
    private final AppProperties properties;

    public AuthController(TokenValidationService tokenValidationService, AppProperties properties) {
        this.tokenValidationService = tokenValidationService;
        this.properties = properties;
    }

    @PostMapping("/token")
    public Map<String, Object> token(Authentication authentication) {
        return Map.of(
            "token", properties.apiBearerToken(),
                "tokenType", "Bearer",
            "expiresIn", "static"
        );
    }
}