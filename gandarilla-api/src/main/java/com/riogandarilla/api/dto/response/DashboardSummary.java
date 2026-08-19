package com.riogandarilla.api.dto.response;

import java.math.BigDecimal;

public record DashboardSummary(
        int mes,
        int anio,
        BigDecimal totalRecaudado,
        int casasPagadas,
        int casasPendientes,
        int movimientosRegistrados,
        int movimientosCancelados,
        int comprobantesEnviados,
        int comprobantesPendientes
) {
}
