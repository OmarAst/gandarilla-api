package com.riogandarilla.api.services;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.services.impl.ReceiptArchiveServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReceiptArchiveServiceImplTest {

    @Test
    void shouldCreatePeriodFolderAndStoreExpectedFilename() throws Exception {
        AppProperties properties = mock(AppProperties.class);
        Path receiptRoot = Path.of("target", "test-output", "recibos");
        when(properties.monthlyArchivePath()).thenReturn(receiptRoot.toString());
        ReceiptArchiveService service = new ReceiptArchiveServiceImpl(properties);

        String archived = service.archiveMonthly(data(), "1-Omar Astorga.pdf", "%PDF-test".getBytes());

        assertThat(archived).isEqualTo("recibos/08-2026/1-Omar Astorga.pdf");
        assertThat(Files.readAllBytes(receiptRoot.resolve("08-2026/1-Omar Astorga.pdf")))
                .isEqualTo("%PDF-test".getBytes());
    }

    private ReceiptDocumentData data() {
        return new ReceiptDocumentData(
                UUID.randomUUID(), 1L, "GAN-2026-000001", 1, 8, "Agosto", 2026,
                "Omar Astorga", "526670000001", new BigDecimal("800.00"),
                "ochocientos pesos 00/100 M.N.", LocalDate.of(2026, 8, 4),
                "Pago de mantenimiento", "Transferencia", "GAN-2026-000001", null,
                "Tesorería", OffsetDateTime.parse("2026-08-05T10:00:00-07:00")
        );
    }
}
