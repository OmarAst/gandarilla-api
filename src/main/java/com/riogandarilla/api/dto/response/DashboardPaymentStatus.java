package com.riogandarilla.api.dto.response;

import java.math.BigDecimal;

public record DashboardPaymentStatus(
        int pagadasATiempo,
        int pagadasConAtraso,
        int vencidas,
        int pendientes,
        int comiteExento,
        int accesoRestringido,
        BigDecimal pendientePorCobrar,
        BigDecimal cuotaAplicable
) {
    public int totalPagadas() {
        return pagadasATiempo + pagadasConAtraso;
    }
}
