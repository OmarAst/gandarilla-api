package com.riogandarilla.api.configs;

import com.riogandarilla.api.configs.properties.AppProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Log4j2
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        List<String> origins = properties.allowedOrigins();
        if (origins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS no puede contener '*'");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "X-Correlation-ID"
        ));
        configuration.setExposedHeaders(List.of(
                "X-Correlation-ID", "X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After"
        ));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        log.info("CORS configurado originsCount={}", origins.size());
        return source;
    }
}
