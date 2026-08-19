package com.riogandarilla.api.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Security security,
        Web web,
        RateLimit rateLimit,
        OpenApi openapi,
        Receipt receipt,
        WhatsApp whatsapp
) {
    public List<String> allowedOrigins() {
        if (cors == null || cors.allowedOrigins() == null || cors.allowedOrigins().isEmpty()) {
            return List.of("http://localhost:3000", "http://localhost:5173");
        }
        return cors.allowedOrigins().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public boolean securityEnabled() {
        return security != null && Boolean.TRUE.equals(security.enabled());
    }

    public String apiBearerToken() {
        return security == null || security.apiBearerToken() == null
                ? ""
                : security.apiBearerToken().trim();
    }

    public List<String> publicPaths() {
        if (security == null || security.publicPaths() == null || security.publicPaths().isEmpty()) {
            return List.of(
                    "/api/health",
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
            );
        }
        return security.publicPaths();
    }

    public String webUsername() {
        return valueOrDefault(web == null ? null : web.username(), "admin");
    }

    public String webPassword() {
        return web == null || web.password() == null ? "" : web.password();
    }

    public boolean rateLimitEnabled() {
        return rateLimit == null || !Boolean.FALSE.equals(rateLimit.enabled());
    }

    public int readRequestsPerMinute() {
        return positiveLimit(rateLimit == null ? null : rateLimit.readRequestsPerMinute(), 120);
    }

    public int writeRequestsPerMinute() {
        return positiveLimit(rateLimit == null ? null : rateLimit.writeRequestsPerMinute(), 20);
    }

    public boolean trustForwardedHeaders() {
        return rateLimit != null && Boolean.TRUE.equals(rateLimit.trustForwardedHeaders());
    }

    public boolean openApiEnabled() {
        return openapi == null || !Boolean.FALSE.equals(openapi.enabled());
    }

    public String receiptConcept() {
        return valueOrDefault(receipt == null ? null : receipt.concept(), "Pago de mantenimiento");
    }

    public String validatedBy() {
        return valueOrDefault(receipt == null ? null : receipt.validatedBy(), "Tesorería");
    }

    public String organizationName() {
        return valueOrDefault(
                receipt == null ? null : receipt.organizationName(),
                "PRIVADA RÍO GANDARILLA A.C."
        );
    }

    public String organizationArea() {
        return valueOrDefault(receipt == null ? null : receipt.organizationArea(), "Tesorería");
    }

    public boolean archiveEnabled() {
        return receipt == null || !Boolean.FALSE.equals(receipt.archiveEnabled());
    }

    public String archivePath() {
        return valueOrDefault(receipt == null ? null : receipt.archivePath(), "generated-receipts");
    }

    public String monthlyArchivePath() {
        return valueOrDefault(receipt == null ? null : receipt.monthlyArchivePath(), "recibos");
    }

    public Duration duplicateWindow() {
        if (receipt == null || receipt.duplicateWindow() == null
                || receipt.duplicateWindow().isNegative() || receipt.duplicateWindow().isZero()) {
            return Duration.ofSeconds(30);
        }
        return receipt.duplicateWindow();
    }

    public String logoPath() {
        return valueOrDefault(receipt == null ? null : receipt.logoPath(), "classpath:branding/logo.png");
    }

    public String backgroundPath() {
        return valueOrDefault(receipt == null ? null : receipt.backgroundPath(), "classpath:branding/fondo.png");
    }

    public String fontPath() {
        return valueOrDefault(receipt == null ? null : receipt.fontPath(), "classpath:fonts/DejaVuSans.ttf");
    }

    public boolean whatsappEnabled() {
        return whatsapp != null && Boolean.TRUE.equals(whatsapp.enabled());
    }

    public String whatsappBaseUrl() {
        return stripTrailingSlash(valueOrDefault(
                whatsapp == null ? null : whatsapp.baseUrl(),
                "https://graph.facebook.com"
        ));
    }

    public String whatsappGraphVersion() {
        return whatsapp == null || whatsapp.graphVersion() == null
                ? ""
                : whatsapp.graphVersion().trim();
    }

    public String whatsappPhoneNumberId() {
        return whatsapp == null || whatsapp.phoneNumberId() == null
                ? ""
                : whatsapp.phoneNumberId().trim();
    }

    public String whatsappAccessToken() {
        return whatsapp == null || whatsapp.accessToken() == null
                ? ""
                : whatsapp.accessToken().trim();
    }

    public String whatsappTemplateName() {
        return valueOrDefault(
                whatsapp == null ? null : whatsapp.templateName(),
                "recibo_pago_mantenimiento"
        );
    }

    public String whatsappLanguageCode() {
        return valueOrDefault(whatsapp == null ? null : whatsapp.languageCode(), "es_MX");
    }

    public Duration whatsappConnectTimeout() {
        return whatsapp == null || whatsapp.connectTimeout() == null
                ? Duration.ofSeconds(5)
                : whatsapp.connectTimeout();
    }

    public Duration whatsappReadTimeout() {
        return whatsapp == null || whatsapp.readTimeout() == null
                ? Duration.ofSeconds(20)
                : whatsapp.readTimeout();
    }

    private int positiveLimit(Integer configured, int defaultValue) {
        if (configured == null) {
            return defaultValue;
        }
        return Math.max(1, Math.min(configured, 100_000));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String stripTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Security(Boolean enabled, String apiBearerToken, List<String> publicPaths) {
    }

    public record Web(String username, String password) {
    }

    public record RateLimit(
            Boolean enabled,
            Integer readRequestsPerMinute,
            Integer writeRequestsPerMinute,
            Boolean trustForwardedHeaders
    ) {
    }

    public record OpenApi(Boolean enabled) {
    }

    public record Receipt(
            String concept,
            String validatedBy,
            String organizationName,
            String organizationArea,
            Boolean archiveEnabled,
            String archivePath,
            String monthlyArchivePath,
            Duration duplicateWindow,
            String logoPath,
            String backgroundPath,
            String fontPath
    ) {
    }

    public record WhatsApp(
            Boolean enabled,
            String baseUrl,
            String graphVersion,
            String phoneNumberId,
            String accessToken,
            String templateName,
            String languageCode,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }
}
