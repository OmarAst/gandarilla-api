package com.riogandarilla.api.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.dto.response.ApiResponse;
import com.riogandarilla.api.dto.response.ErrorResponse;
import com.riogandarilla.api.utils.RequestIdSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.rateLimitEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LimitPolicy policy = resolvePolicy(request);
        int limit = policy.limit();
        long currentWindow = Instant.now().getEpochSecond() / WINDOW_SECONDS;
        String key = clientKey(request) + ':' + policy.bucket() + ':' + currentWindow;

        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.window() != currentWindow) {
                return new WindowCounter(currentWindow, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        int used = counter.count().get();
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - used)));

        if (used > limit) {
            long retryAfter = WINDOW_SECONDS - (Instant.now().getEpochSecond() % WINDOW_SECONDS);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            writeTooManyRequests(request, response);
            cleanup(currentWindow);
            return;
        }

        filterChain.doFilter(request, response);
        cleanup(currentWindow);
    }

    private LimitPolicy resolvePolicy(HttpServletRequest request) {
        return switch (request.getMethod().toUpperCase()) {
            case "GET", "HEAD" -> new LimitPolicy("read", properties.readRequestsPerMinute());
            default -> new LimitPolicy("write", properties.writeRequestsPerMinute());
        };
    }

    private String clientKey(HttpServletRequest request) {
        if (properties.trustForwardedHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("Rate limit excedido method={} path={}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = new ErrorResponse(
                request.getRequestURI(),
                "RATE_LIMIT_EXCEEDED",
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                List.of("Se excedió el número de solicitudes permitido. Intenta nuevamente más tarde."),
                RequestIdSupport.current(),
                OffsetDateTime.now()
        );
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes", error)
        );
    }

    private void cleanup(long currentWindow) {
        if (counters.size() > 10_000) {
            counters.entrySet().removeIf(entry -> entry.getValue().window() < currentWindow - 1);
        }
    }

    private record LimitPolicy(String bucket, int limit) {
    }

    private record WindowCounter(long window, AtomicInteger count) {
    }
}
