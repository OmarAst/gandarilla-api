package com.riogandarilla.api.services;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.services.impl.PdfReceiptServiceImpl;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfReceiptServiceImplTest {

    @Test
    void shouldGeneratePdfWithUnicodeAndRemoveControlCharacters() throws Exception {
        AppProperties properties = mock(AppProperties.class);
        when(properties.organizationName()).thenReturn("PRIVADA RÍO GANDARILLA A.C.");
        when(properties.organizationArea()).thenReturn("Tesorería");
        when(properties.logoPath()).thenReturn("classpath:branding/logo.png");
        when(properties.backgroundPath()).thenReturn("classpath:branding/fondo.png");
        when(properties.fontPath()).thenReturn("classpath:fonts/DejaVuSans.ttf");

        PdfReceiptService service = new PdfReceiptServiceImpl(properties, new DefaultResourceLoader());
        ReceiptDocumentData data = new ReceiptDocumentData(
                UUID.randomUUID(),
                125L,
                "GAN-2026-000125",
                42,
                7,
                "Julio",
                2026,
                "José Núñez — casa feliz\u008D",
                "526670000042",
                new BigDecimal("900.00"),
                "novecientos pesos 00/100 M.N.",
                LocalDate.of(2026, 7, 7),
                "Pago de mantenimiento",
                "Transferencia",
                "",
                "",
                "Tesorería",
                OffsetDateTime.parse("2026-07-07T12:00:00-07:00")
        );

        byte[] pdf = service.generate(data);
        assertThat(pdf).startsWith("%PDF".getBytes());
        assertThat(pdf.length).isGreaterThan(1_000);
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("PRIVADA RÍO GANDARILLA A.C.", "Tesorería", "José Núñez — casa feliz");
            assertThat(text).doesNotContain("\u008D");
        }
    }
}
