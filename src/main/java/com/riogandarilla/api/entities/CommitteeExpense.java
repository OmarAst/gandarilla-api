package com.riogandarilla.api.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CommitteeExpense(
        Long id,
        String concepto,
        String proveedor,
        BigDecimal monto,
        LocalDate fechaGasto,
        int mes,
        int anio,
        String estatus
) {
}
