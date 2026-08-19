package com.riogandarilla.api.dto.response;

import java.math.BigDecimal;

public record DashboardSummary(
        int mes,
        int anio,
        BigDecimal totalRecaudado,
        BigDecimal gastosCubiertos,
        BigDecimal gastosPendientes,
        BigDecimal disponibleComiteActual,
        BigDecimal pendientePorCobrar,
        int casasPagadas,
        int casasPendientes,
        int movimientosRegistrados,
        int movimientosCancelados
) {
}
