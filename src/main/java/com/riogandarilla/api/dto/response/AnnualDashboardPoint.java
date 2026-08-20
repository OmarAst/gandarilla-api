package com.riogandarilla.api.dto.response;

import java.math.BigDecimal;

public record AnnualDashboardPoint(
        int mes,
        String nombreMes,
        BigDecimal totalRecaudado,
        int pagadasATiempo,
        int pagadasConAtraso,
        int vencidas,
        int pendientes,
        int comiteExento,
        int recaudadoPorcentaje
) {
}
