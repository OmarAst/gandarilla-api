package com.riogandarilla.api.configs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@OpenAPIDefinition(info = @Info(
        title = "Privada Río Gandarilla - Pagos y Recibos API",
        version = "1.0.0",
        description = "API REST para registrar movimientos de pago, generar comprobantes y enviarlos mediante WhatsApp Cloud API."
))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "API token",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
