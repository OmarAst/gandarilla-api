package com.riogandarilla.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.entities.WhatsAppSendResult;
import com.riogandarilla.api.exception.ExternalServiceException;
import com.riogandarilla.api.services.impl.MetaWhatsAppService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class MetaWhatsAppServiceTest {

    @Test
    void shouldSimulateWithoutCallingMetaWhenDisabled() {
        AppProperties properties = properties(false);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppService service = new MetaWhatsAppService(properties, builder.build(), new ObjectMapper());

        WhatsAppSendResult result = service.sendReceipt(data("+52 667-000-0042"), "%PDF".getBytes(), "receipt.pdf");

        assertThat(result.status()).isEqualTo("SIMULATED");
        assertThat(result.messageId()).startsWith("mock-");
        server.verify();
    }

    @Test
    void shouldUploadDocumentAndBuildApprovedTemplatePayload() {
        AppProperties properties = properties(true);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://graph.example/v22.0/123/media"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withSuccess("{\"id\":\"media-1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.example/v22.0/123/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "messaging_product":"whatsapp",
                          "recipient_type":"individual",
                          "to":"526670000042",
                          "type":"template",
                          "template":{
                            "name":"recibo_pago_mantenimiento",
                            "language":{"code":"es_MX"},
                            "components":[
                              {"type":"header","parameters":[{"type":"document","document":{"id":"media-1","filename":"receipt.pdf"}}]},
                              {"type":"body","parameters":[
                                {"type":"text","text":"José Núñez"},
                                {"type":"text","text":"Agosto"},
                                {"type":"text","text":"2026"}
                              ]}
                            ]
                          }
                        }
                        """, true))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.123\"}]}", MediaType.APPLICATION_JSON));
        WhatsAppService service = new MetaWhatsAppService(properties, builder.build(), new ObjectMapper());

        WhatsAppSendResult result = service.sendReceipt(data("+52 667-000-0042"), "%PDF".getBytes(), "receipt.pdf");

        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.messageId()).isEqualTo("wamid.123");
        server.verify();
    }

    @Test
    void shouldClassifyMetaAuthenticationFailureWithoutExposingToken() {
        AppProperties properties = properties(true);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://graph.example/v22.0/123/media"))
                .andRespond(withUnauthorizedRequest());
        WhatsAppService service = new MetaWhatsAppService(properties, builder.build(), new ObjectMapper());

        assertThatThrownBy(() -> service.sendReceipt(data("526670000042"), "%PDF".getBytes(), "receipt.pdf"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageNotContaining("secret-token")
                .extracting(error -> ((ExternalServiceException) error).code())
                .isEqualTo("WHATSAPP_UNAUTHORIZED");
    }

    private AppProperties properties(boolean enabled) {
        AppProperties properties = mock(AppProperties.class);
        when(properties.whatsappEnabled()).thenReturn(enabled);
        when(properties.whatsappBaseUrl()).thenReturn("https://graph.example");
        when(properties.whatsappGraphVersion()).thenReturn("v22.0");
        when(properties.whatsappPhoneNumberId()).thenReturn("123");
        when(properties.whatsappAccessToken()).thenReturn("secret-token");
        when(properties.whatsappTemplateName()).thenReturn("recibo_pago_mantenimiento");
        when(properties.whatsappLanguageCode()).thenReturn("es_MX");
        return properties;
    }

    private ReceiptDocumentData data(String phone) {
        return new ReceiptDocumentData(
                UUID.randomUUID(), 1L, "GAN-2026-000001", 1, 8, "Agosto", 2026,
                "José Núñez", phone, new BigDecimal("800.00"), "ochocientos pesos 00/100 M.N.",
                LocalDate.of(2026, 8, 4), "Pago de mantenimiento", "Transferencia",
                "GAN-2026-000001", "Pago de agosto", "Tesorería",
                OffsetDateTime.parse("2026-08-05T10:00:00-07:00")
        );
    }
}
