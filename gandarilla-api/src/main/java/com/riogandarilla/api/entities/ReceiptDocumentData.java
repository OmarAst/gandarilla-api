package com.riogandarilla.api.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceiptDocumentData(
        UUID receiptId,
        Long movementId,
        String folio,
        int casa,
        int mes,
        String mesNombre,
        int anio,
        String titular,
        String telefono,
        BigDecimal monto,
        String montoConLetra,
        LocalDate fechaPago,
        String concepto,
        String formaPago,
        String referencia,
        String observaciones,
        String validadoPor,
        OffsetDateTime generatedAt
) {
}
