package com.riogandarilla.api.controllers;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final AppProperties properties;

    public HealthController(AppProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "gandarilla-api",
                "whatsappMode", properties.whatsappEnabled() ? "LIVE" : "SIMULATED",
                "timestamp", OffsetDateTime.now()
        ));
    }
}
