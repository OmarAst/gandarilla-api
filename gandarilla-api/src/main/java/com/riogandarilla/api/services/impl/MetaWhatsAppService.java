package com.riogandarilla.api.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.entities.WhatsAppSendResult;
import com.riogandarilla.api.exception.ExternalServiceException;
import com.riogandarilla.api.exception.WhatsAppIntegrationException;
import com.riogandarilla.api.services.WhatsAppService;
import com.riogandarilla.api.utils.PhoneNumberSupport;
import com.riogandarilla.api.utils.RequestIdSupport;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
public class MetaWhatsAppService implements WhatsAppService {

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MetaWhatsAppService(AppProperties properties, RestClient whatsappRestClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = whatsappRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public WhatsAppSendResult sendReceipt(ReceiptDocumentData data, byte[] pdf, String filename) {
        if (!properties.whatsappEnabled()) {
            String mockId = "mock-" + UUID.randomUUID();
            log.info("Envío WhatsApp simulado receiptId={} house={} messageId={}",
                    data.receiptId(), data.casa(), mockId);
            return new WhatsAppSendResult("SIMULATED", mockId, null);
        }

        try {
            String mediaId = uploadPdf(pdf, filename);
            String messageId = sendTemplate(data, mediaId, filename);
            return new WhatsAppSendResult("SENT", messageId, mediaId);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.error("WhatsApp rechazó el envío requestId={} movementId={} house={} status={}",
                    RequestIdSupport.current(), data.movementId(), data.casa(), status);
            throw mapHttpError(status);
        } catch (RestClientException exception) {
            log.error("WhatsApp no disponible requestId={} movementId={} house={}",
                    RequestIdSupport.current(), data.movementId(), data.casa());
            throw new ExternalServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "WHATSAPP_UNAVAILABLE",
                    "No fue posible conectar con WhatsApp en este momento"
            );
        }
    }

    private String uploadPdf(byte[] pdf, String filename) {
        ByteArrayResource fileResource = new ByteArrayResource(pdf) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_PDF);
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(fileResource, fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("messaging_product", "whatsapp");
        body.add("type", MediaType.APPLICATION_PDF_VALUE);
        body.add("file", filePart);

        String response = restClient.post()
                .uri(endpoint("media"))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = parse(response, "respuesta de carga de archivo");
        String mediaId = root.path("id").asText("");
        if (mediaId.isBlank()) {
            throw new WhatsAppIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "WHATSAPP_MEDIA_ID_MISSING",
                    "WhatsApp no devolvió el identificador del documento"
            );
        }
        return mediaId;
    }

    private String sendTemplate(ReceiptDocumentData data, String mediaId, String filename) {
        Map<String, Object> headerComponent = Map.of(
                "type", "header",
                "parameters", List.of(Map.of(
                        "type", "document",
                        "document", Map.of("id", mediaId, "filename", filename)
                ))
        );

        Map<String, Object> bodyComponent = Map.of(
                "type", "body",
                "parameters", List.of(
                        textParameter(data.titular()),
                        textParameter(data.mesNombre()),
                        textParameter(String.valueOf(data.anio()))
                )
        );

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", PhoneNumberSupport.normalizeInternational(data.telefono()),
                "type", "template",
                "template", Map.of(
                        "name", properties.whatsappTemplateName(),
                        "language", Map.of("code", properties.whatsappLanguageCode()),
                        "components", List.of(headerComponent, bodyComponent)
                )
        );

        String response = restClient.post()
                .uri(endpoint("messages"))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        JsonNode root = parse(response, "respuesta de envío");
        JsonNode messages = root.path("messages");
        String messageId = messages.isArray() && !messages.isEmpty()
                ? messages.get(0).path("id").asText("")
                : "";
        if (messageId.isBlank()) {
            throw new WhatsAppIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "WHATSAPP_MESSAGE_ID_MISSING",
                    "WhatsApp no devolvió el identificador del mensaje"
            );
        }
        return messageId;
    }

    private Map<String, String> textParameter(String value) {
        return Map.of("type", "text", "text", value);
    }

    private JsonNode parse(String response, String context) {
        if (response == null || response.isBlank()) {
            throw new WhatsAppIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "WHATSAPP_EMPTY_RESPONSE",
                    "WhatsApp devolvió una respuesta vacía"
            );
        }
        try {
            return objectMapper.readTree(response);
        } catch (Exception exception) {
            log.error("No fue posible interpretar {}", context, exception);
            throw new WhatsAppIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "WHATSAPP_INVALID_RESPONSE",
                    "WhatsApp devolvió una respuesta no reconocida"
            );
        }
    }

    private String endpoint(String resource) {
        return properties.whatsappBaseUrl()
                + "/" + properties.whatsappGraphVersion()
                + "/" + properties.whatsappPhoneNumberId()
                + "/" + resource;
    }

    private String bearer() {
        return "Bearer " + properties.whatsappAccessToken();
    }

    private ExternalServiceException mapHttpError(int status) {
        if (status == 429) {
            return new ExternalServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "WHATSAPP_RATE_LIMITED",
                    "WhatsApp limitó temporalmente las solicitudes; intenta más tarde"
            );
        }
        String code = switch (status) {
            case 400 -> "WHATSAPP_BAD_REQUEST";
            case 401 -> "WHATSAPP_UNAUTHORIZED";
            case 403 -> "WHATSAPP_FORBIDDEN";
            case 404 -> "WHATSAPP_RESOURCE_NOT_FOUND";
            default -> status >= 500 ? "WHATSAPP_UPSTREAM_ERROR" : "WHATSAPP_API_ERROR";
        };
        return new ExternalServiceException(
                HttpStatus.BAD_GATEWAY,
                code,
                "WhatsApp no pudo procesar el envío del comprobante"
        );
    }
}
