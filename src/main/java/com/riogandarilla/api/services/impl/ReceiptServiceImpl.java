package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.dto.request.SendReceiptRequest;
import com.riogandarilla.api.dto.request.GenerateMonthlyReceiptsRequest;
import com.riogandarilla.api.dto.response.GeneratedReceiptResponse;
import com.riogandarilla.api.dto.response.MonthlyReceiptsResponse;
import com.riogandarilla.api.dto.response.SendReceiptResponse;
import com.riogandarilla.api.entities.PaymentMethod;
import com.riogandarilla.api.entities.PaymentMovement;
import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.entities.WhatsAppSendResult;
import com.riogandarilla.api.exception.BusinessException;
import com.riogandarilla.api.exception.ConflictException;
import com.riogandarilla.api.exception.ResourceNotFoundException;
import com.riogandarilla.api.repositories.PaymentMovementRepository;
import com.riogandarilla.api.services.PdfReceiptService;
import com.riogandarilla.api.services.ReceiptArchiveService;
import com.riogandarilla.api.services.ReceiptService;
import com.riogandarilla.api.services.WhatsAppService;
import com.riogandarilla.api.utils.MoneyToWordsSpanish;
import com.riogandarilla.api.utils.PhoneNumberSupport;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final Locale SPANISH = Locale.forLanguageTag("es-MX");

    private final PaymentMovementRepository movementRepository;
    private final PdfReceiptService pdfReceiptService;
    private final ReceiptArchiveService archiveService;
    private final WhatsAppService whatsAppService;
    private final AppProperties properties;
    private final Clock clock;

    public ReceiptServiceImpl(
            PaymentMovementRepository movementRepository,
            PdfReceiptService pdfReceiptService,
            ReceiptArchiveService archiveService,
            WhatsAppService whatsAppService,
            AppProperties properties,
            Clock clock
    ) {
        this.movementRepository = movementRepository;
        this.pdfReceiptService = pdfReceiptService;
        this.archiveService = archiveService;
        this.whatsAppService = whatsAppService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public SendReceiptResponse generateAndSend(long movementId) {
        PaymentMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAYMENT_MOVEMENT_NOT_FOUND",
                        "No se encontró el movimiento de pago " + movementId
                ));
        return process(movement);
    }

    @Override
    public SendReceiptResponse generateAndSend(SendReceiptRequest request) {
        int currentYear = OffsetDateTime.ofInstant(clock.instant(), clock.getZone()).getYear();
        PaymentMovement movement = movementRepository.findLatestRegisteredByHouseAndPeriod(
                        request.casa(),
                        request.mes(),
                        currentYear
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAYMENT_MOVEMENT_NOT_FOUND",
                        "No se encontró un pago registrado para la casa " + request.casa()
                                + ", mes " + request.mes() + " y año " + currentYear
                ));
        return process(movement);
    }

    @Override
    public MonthlyReceiptsResponse generateMonthly(GenerateMonthlyReceiptsRequest request) {
        List<GeneratedReceiptResponse> receipts = movementRepository
                .findPaidResidentsByPeriod(request.mes(), request.anio())
                .stream()
                .map(this::generateMonthlyReceipt)
                .toList();
        String folder = properties.monthlyArchivePath().replace('\\', '/')
                + "/" + String.format("%02d-%04d", request.mes(), request.anio());

        log.info("Comprobantes mensuales generados period={}/{} total={} folder={}",
                request.mes(), request.anio(), receipts.size(), folder);
        return new MonthlyReceiptsResponse(
                request.mes(), request.anio(), folder, receipts.size(), receipts
        );
    }

    private GeneratedReceiptResponse generateMonthlyReceipt(PaymentMovement movement) {
        ReceiptDocumentData data = toDocumentData(movement);
        String filename = monthlyFilename(movement);
        byte[] pdf = pdfReceiptService.generate(data);
        String archivedPath = archiveService.archiveMonthly(data, filename, pdf);
        return new GeneratedReceiptResponse(
                movement.id(), movement.folio(), movement.numCasa(),
                movement.residenteNombre(), archivedPath
        );
    }

    private SendReceiptResponse process(PaymentMovement movement) {
        validateMovement(movement);

        ReceiptDocumentData data = toDocumentData(movement);
        String monthName = data.mesNombre();
        UUID receiptId = data.receiptId();
        OffsetDateTime generatedAt = data.generatedAt();

        UUID reservation = UUID.randomUUID();
        if (!movementRepository.reserveWhatsAppSend(movement.id(), reservation, generatedAt)) {
            throw new ConflictException(
                    "RECEIPT_SEND_IN_PROGRESS",
                    "El comprobante ya fue enviado o tiene un envío en proceso"
            );
        }
        try {
            byte[] pdf = pdfReceiptService.generate(data);
            String filename = filename(data);
            String archivedPath = archiveService.archive(data, filename, pdf);
            WhatsAppSendResult sendResult = whatsAppService.sendReceipt(data, pdf, filename);
            String deliveryStatus = persistDeliveryStatus(movement, reservation, sendResult, generatedAt);

            log.info(
                    "Recibo procesado receiptId={} movementId={} folio={} house={} whatsappStatus={}",
                    receiptId,
                    movement.id(),
                    movement.folio(),
                    movement.numCasa(),
                    deliveryStatus
            );

            return new SendReceiptResponse(
                    receiptId,
                    movement.id(),
                    movement.folio(),
                    movement.numCasa(),
                    movement.mes(),
                    monthName,
                    movement.anio(),
                    movement.residenteNombre(),
                    PhoneNumberSupport.mask(movement.residenteTelefono()),
                    movement.monto(),
                    filename,
                    archivedPath,
                    deliveryStatus,
                    sendResult.messageId(),
                    generatedAt
            );
        } catch (RuntimeException exception) {
            movementRepository.releaseWhatsAppSend(movement.id(), reservation);
            throw exception;
        }
    }

    private ReceiptDocumentData toDocumentData(PaymentMovement movement) {
        Month month = Month.of(movement.mes());
        String monthName = capitalize(month.getDisplayName(TextStyle.FULL, SPANISH));
        PaymentMethod paymentMethod = PaymentMethod.fromId(movement.formaPago());
        return new ReceiptDocumentData(
                UUID.randomUUID(), movement.id(), movement.folio(), movement.numCasa(),
                movement.mes(), monthName, movement.anio(), movement.residenteNombre(),
                movement.residenteTelefono(), movement.monto(),
                MoneyToWordsSpanish.pesos(movement.monto()), movement.fechaPago(),
                properties.receiptConcept(), paymentMethod.descripcion(), movement.folio(),
                movement.observaciones(), properties.validatedBy(),
                OffsetDateTime.ofInstant(clock.instant(), clock.getZone())
        );
    }

    private String monthlyFilename(PaymentMovement movement) {
        String resident = movement.residenteNombre() == null ? "" : movement.residenteNombre()
                .replaceAll("[<>:\"/\\\\|?*\\p{Cc}\\p{Cf}]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("[. ]+$", "");
        if (resident.isBlank()) {
            resident = "residente-" + movement.residenteId();
        }
        return movement.numCasa() + "-" + resident + ".pdf";
    }

    private String persistDeliveryStatus(
            PaymentMovement movement,
            UUID reservation,
            WhatsAppSendResult sendResult,
            OffsetDateTime generatedAt
    ) {
        if (!"SENT".equalsIgnoreCase(sendResult.status())) {
            movementRepository.releaseWhatsAppSend(movement.id(), reservation);
            return sendResult.status();
        }

        boolean updated = movementRepository.markWhatsAppSent(
                movement.id(),
                reservation,
                sendResult.messageId(),
                generatedAt
        );
        if (!updated) {
            log.error(
                    "El mensaje fue enviado, pero no se actualizó el movimiento movementId={} messageId={}",
                    movement.id(),
                    sendResult.messageId()
            );
            return "SENT_UNTRACKED";
        }
        return "SENT";
    }

    private void validateMovement(PaymentMovement movement) {
        if (!"REGISTRADO".equalsIgnoreCase(movement.estatus())) {
            throw new BusinessException(
                    "PAYMENT_MOVEMENT_NOT_ACTIVE",
                    "El movimiento " + movement.id() + " no se encuentra registrado"
            );
        }
        if (!movement.residenteActivo()) {
            throw new BusinessException(
                    "RESIDENT_INACTIVE",
                    "El residente de la casa " + movement.numCasa() + " está inactivo"
            );
        }
        if (movement.whatsappEnviado()) {
            throw new ConflictException(
                    "RECEIPT_ALREADY_SENT",
                    "El comprobante del movimiento " + movement.id() + " ya fue enviado por WhatsApp"
            );
        }
        if (!PhoneNumberSupport.isValidInternational(movement.residenteTelefono())) {
            throw new BusinessException(
                    "RESIDENT_PHONE_INVALID",
                    "La casa " + movement.numCasa() + " no tiene un número de WhatsApp internacional válido"
            );
        }
    }

    private String filename(ReceiptDocumentData data) {
        String safeFolio = data.folio().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-");
        return "recibo-casa-" + data.casa() + "-" + safeFolio
                + "-" + data.receiptId() + ".pdf";
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
