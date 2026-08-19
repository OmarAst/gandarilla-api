package com.riogandarilla.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PaymentMovementResponse(
        Long id,
        String folio,
        Long residenteId,
        String residente,
        Integer casa,
        BigDecimal monto,
        String observaciones,
        PaymentMethodResponse formaPago,
        LocalDate fechaPago,
        Integer mes,
        Integer anio,
        String estatus,
        boolean whatsappEnviado,
        OffsetDateTime fechaEnvioWhatsapp,
        String whatsappMessageId,
        OffsetDateTime fechaCreacion
) {
}
