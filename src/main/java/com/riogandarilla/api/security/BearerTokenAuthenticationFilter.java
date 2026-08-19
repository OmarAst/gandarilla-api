package com.riogandarilla.api.security;

import com.riogandarilla.api.configs.properties.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AppProperties properties;
    private final TokenValidationService tokenValidationService;
    private final SecurityErrorWriter errorWriter;

    public BearerTokenAuthenticationFilter(
            AppProperties properties,
            TokenValidationService tokenValidationService,
            SecurityErrorWriter errorWriter
    ) {
        this.properties = properties;
        this.tokenValidationService = tokenValidationService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.securityEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        if ("/api/auth/token".equals(path)) {
            return true;
        }
        return properties.publicPaths().stream().anyMatch(pattern -> matches(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    "MISSING_BEARER_TOKEN", "Se requiere el encabezado Authorization: Bearer <token>");
            return;
        }

        String token = authorization.substring(7).trim();
        if (!tokenValidationService.isValid(token)) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    "INVALID_BEARER_TOKEN", "El token Bearer no es válido");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal-api-client",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean matches(String pattern, String path) {
        return new org.springframework.util.AntPathMatcher().match(pattern, path);
    }
}
