package com.riogandarilla.api.services;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.dto.response.SendReceiptResponse;
import com.riogandarilla.api.dto.request.GenerateMonthlyReceiptsRequest;
import com.riogandarilla.api.dto.response.MonthlyReceiptsResponse;
import com.riogandarilla.api.entities.PaymentMovement;
import com.riogandarilla.api.entities.WhatsAppSendResult;
import com.riogandarilla.api.exception.ConflictException;
import com.riogandarilla.api.exception.ExternalServiceException;
import com.riogandarilla.api.repositories.PaymentMovementRepository;
import com.riogandarilla.api.services.impl.ReceiptServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.when;

class ReceiptServiceImplTest {

    @Test
    void shouldGenerateAndSendReceiptByMovementId() {
        PaymentMovementRepository repository = mock(PaymentMovementRepository.class);
        PdfReceiptService pdfService = mock(PdfReceiptService.class);
        ReceiptArchiveService archiveService = mock(ReceiptArchiveService.class);
        WhatsAppService whatsAppService = mock(WhatsAppService.class);
        AppProperties properties = mock(AppProperties.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-05T03:00:00Z"),
                ZoneId.of("America/Mazatlan")
        );

        PaymentMovement movement = new PaymentMovement(
                125L,
                42L,
                "Daniel Salazar Sánchez",
                "526670000042",
                42,
                true,
                new BigDecimal("900.00"),
                "Pago correspondiente a julio",
                1,
                LocalDate.of(2026, 7, 7),
                7,
                2026,
                "GAN-2026-000125",
                "REGISTRADO",
                false,
                null,
                null,
                OffsetDateTime.parse("2026-07-07T12:00:00-07:00"),
                OffsetDateTime.parse("2026-07-07T12:00:00-07:00")
        );

        when(repository.findById(125L)).thenReturn(Optional.of(movement));
        when(properties.receiptConcept()).thenReturn("Pago de mantenimiento");
        when(properties.validatedBy()).thenReturn("Tesorería");
        when(repository.reserveWhatsAppSend(eq(125L), any(), any())).thenReturn(true);
        when(pdfService.generate(any())).thenReturn("%PDF-test".getBytes());
        when(archiveService.archive(any(), anyString(), any())).thenReturn("2026/07/receipt.pdf");
        when(whatsAppService.sendReceipt(any(), any(), anyString()))
                .thenReturn(new WhatsAppSendResult("SIMULATED", "mock-id", null));

        ReceiptService service = new ReceiptServiceImpl(
                repository,
                pdfService,
                archiveService,
                whatsAppService,
                properties,
                clock
        );

        SendReceiptResponse response = service.generateAndSend(125L);

        assertThat(response.movementId()).isEqualTo(125L);
        assertThat(response.folio()).isEqualTo("GAN-2026-000125");
        assertThat(response.casa()).isEqualTo(42);
        assertThat(response.mesNombre()).isEqualTo("Julio");
        assertThat(response.estadoEnvio()).isEqualTo("SIMULATED");
        assertThat(response.telefonoDestino()).endsWith("0042");
        verify(repository).releaseWhatsAppSend(eq(125L), any());
    }

    @Test
    void shouldNotSendWhenAnotherRequestOwnsThePersistentReservation() {
        Fixture fixture = fixture();
        when(fixture.repository.reserveWhatsAppSend(eq(125L), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> fixture.service.generateAndSend(125L))
                .isInstanceOf(ConflictException.class);
        verify(fixture.whatsAppService, never()).sendReceipt(any(), any(), anyString());
    }

    @Test
    void shouldReleaseReservationAndNotMarkSentWhenMetaFails() {
        Fixture fixture = fixture();
        when(fixture.repository.reserveWhatsAppSend(eq(125L), any(), any())).thenReturn(true);
        when(fixture.pdfService.generate(any())).thenReturn("%PDF-test".getBytes());
        when(fixture.archiveService.archive(any(), anyString(), any())).thenReturn("receipt.pdf");
        when(fixture.whatsAppService.sendReceipt(any(), any(), anyString()))
                .thenThrow(new ExternalServiceException(HttpStatus.BAD_GATEWAY, "WHATSAPP_UPSTREAM_ERROR", "falló"));

        assertThatThrownBy(() -> fixture.service.generateAndSend(125L))
                .isInstanceOf(ExternalServiceException.class);
        verify(fixture.repository).releaseWhatsAppSend(eq(125L), any());
        verify(fixture.repository, never()).markWhatsAppSent(any(Long.class), any(), anyString(), any());
    }

    @Test
    void shouldMarkSentOnlyAfterMetaReturnsWamid() {
        Fixture fixture = fixture();
        when(fixture.repository.reserveWhatsAppSend(eq(125L), any(), any())).thenReturn(true);
        when(fixture.pdfService.generate(any())).thenReturn("%PDF-test".getBytes());
        when(fixture.archiveService.archive(any(), anyString(), any())).thenReturn("receipt.pdf");
        when(fixture.whatsAppService.sendReceipt(any(), any(), anyString()))
                .thenReturn(new WhatsAppSendResult("SENT", "wamid.123", "media-1"));
        when(fixture.repository.markWhatsAppSent(eq(125L), any(), eq("wamid.123"), any())).thenReturn(true);

        SendReceiptResponse response = fixture.service.generateAndSend(125L);

        assertThat(response.estadoEnvio()).isEqualTo("SENT");
        assertThat(response.whatsappMessageId()).isEqualTo("wamid.123");
        verify(fixture.repository).markWhatsAppSent(eq(125L), any(), eq("wamid.123"), any());
    }

    @Test
    void shouldGenerateMonthlyReceiptsWithoutSendingWhatsApp() {
        Fixture fixture = fixture();
        PaymentMovement second = new PaymentMovement(
                126L, 43L, "María López", "526670000043", 43, true,
                new BigDecimal("900.00"), "Pago", 2, LocalDate.of(2026, 7, 8), 7, 2026,
                "GAN-2026-000126", "REGISTRADO", false, null, null,
                OffsetDateTime.parse("2026-07-08T12:00:00-07:00"),
                OffsetDateTime.parse("2026-07-08T12:00:00-07:00")
        );
        when(fixture.repository.findPaidResidentsByPeriod(7, 2026))
                .thenReturn(java.util.List.of(movement(), second));
        when(fixture.pdfService.generate(any())).thenReturn("%PDF-test".getBytes());
        when(fixture.archiveService.archiveMonthly(any(), eq("42-José Núñez.pdf"), any()))
                .thenReturn("recibos/07-2026/42-José Núñez.pdf");
        when(fixture.archiveService.archiveMonthly(any(), eq("43-María López.pdf"), any()))
                .thenReturn("recibos/07-2026/43-María López.pdf");
        when(fixture.properties.monthlyArchivePath()).thenReturn("recibos");

        MonthlyReceiptsResponse response = fixture.service.generateMonthly(
                new GenerateMonthlyReceiptsRequest(7, 2026)
        );

        assertThat(response.total()).isEqualTo(2);
        assertThat(response.carpeta()).isEqualTo("recibos/07-2026");
        assertThat(response.recibos()).extracting(item -> item.archivo())
                .containsExactly(
                        "recibos/07-2026/42-José Núñez.pdf",
                        "recibos/07-2026/43-María López.pdf"
                );
        verify(fixture.pdfService, times(2)).generate(any());
        verify(fixture.whatsAppService, never()).sendReceipt(any(), any(), anyString());
        verify(fixture.repository, never()).markWhatsAppSent(any(Long.class), any(), anyString(), any());
    }

    private Fixture fixture() {
        PaymentMovementRepository repository = mock(PaymentMovementRepository.class);
        PdfReceiptService pdfService = mock(PdfReceiptService.class);
        ReceiptArchiveService archiveService = mock(ReceiptArchiveService.class);
        WhatsAppService whatsAppService = mock(WhatsAppService.class);
        AppProperties properties = mock(AppProperties.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-05T03:00:00Z"), ZoneId.of("America/Mazatlan"));
        PaymentMovement movement = movement();
        when(repository.findById(125L)).thenReturn(Optional.of(movement));
        when(properties.receiptConcept()).thenReturn("Pago de mantenimiento");
        when(properties.validatedBy()).thenReturn("Tesorería");
        ReceiptService service = new ReceiptServiceImpl(
                repository, pdfService, archiveService, whatsAppService, properties, clock
        );
        return new Fixture(repository, pdfService, archiveService, whatsAppService, properties, service);
    }

    private PaymentMovement movement() {
        return new PaymentMovement(
                125L, 42L, "José Núñez", "526670000042", 42, true,
                new BigDecimal("900.00"), "Pago", 1, LocalDate.of(2026, 7, 7), 7, 2026,
                "GAN-2026-000125", "REGISTRADO", false, null, null,
                OffsetDateTime.parse("2026-07-07T12:00:00-07:00"),
                OffsetDateTime.parse("2026-07-07T12:00:00-07:00")
        );
    }

    private record Fixture(
            PaymentMovementRepository repository,
            PdfReceiptService pdfService,
            ReceiptArchiveService archiveService,
            WhatsAppService whatsAppService,
            AppProperties properties,
            ReceiptService service
    ) {
    }
}
