package com.riogandarilla.api.dto.response;

import java.util.List;

public record MonthlyReceiptsResponse(
        int mes,
        int anio,
        String carpeta,
        int total,
        List<GeneratedReceiptResponse> recibos
) {
}
