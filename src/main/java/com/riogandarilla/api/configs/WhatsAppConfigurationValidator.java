package com.riogandarilla.api.configs;

import com.riogandarilla.api.configs.properties.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppConfigurationValidator {

    private final AppProperties properties;

    public WhatsAppConfigurationValidator(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        if (!properties.whatsappEnabled()) {
            return;
        }
        if (properties.whatsappGraphVersion().isBlank()
                || properties.whatsappPhoneNumberId().isBlank()
                || properties.whatsappAccessToken().isBlank()
                || properties.whatsappTemplateName().isBlank()) {
            throw new IllegalStateException(
                    "WHATSAPP_GRAPH_VERSION, WHATSAPP_PHONE_NUMBER_ID, WHATSAPP_ACCESS_TOKEN y "
                            + "WHATSAPP_TEMPLATE_NAME son obligatorios cuando WHATSAPP_ENABLED=true"
            );
        }
        if (!properties.whatsappGraphVersion().matches("^v[0-9]+\\.[0-9]+$")) {
            throw new IllegalStateException("WHATSAPP_GRAPH_VERSION debe tener el formato vNN.N");
        }
        if (!properties.whatsappPhoneNumberId().matches("^[0-9]+$")) {
            throw new IllegalStateException("WHATSAPP_PHONE_NUMBER_ID debe contener solo dígitos");
        }
    }
}
