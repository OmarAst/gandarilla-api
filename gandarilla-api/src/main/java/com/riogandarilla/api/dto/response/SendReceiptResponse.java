package com.riogandarilla.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SendReceiptResponse(
        UUID receiptId,
        Long movementId,
        String folio,
        int casa,
        int mes,
        String mesNombre,
        int anio,
        String titular,
        String telefonoDestino,
        BigDecimal monto,
        String archivo,
        String archivoArchivado,
        String estadoEnvio,
        String whatsappMessageId,
        OffsetDateTime generatedAt
) {
}
