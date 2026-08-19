package com.riogandarilla.api.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PaymentMovement(
        Long id,
        Long residenteId,
        String residenteNombre,
        String residenteTelefono,
        Integer numCasa,
        boolean residenteActivo,
        BigDecimal monto,
        String observaciones,
        Integer formaPago,
        LocalDate fechaPago,
        Integer mes,
        Integer anio,
        String folio,
        String estatus,
        boolean whatsappEnviado,
        OffsetDateTime fechaEnvioWhatsapp,
        String whatsappMessageId,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
