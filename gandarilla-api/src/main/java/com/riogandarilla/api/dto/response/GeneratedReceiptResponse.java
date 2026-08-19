package com.riogandarilla.api.dto.response;

public record GeneratedReceiptResponse(
        Long movementId,
        String folio,
        int casa,
        String residente,
        String archivo
) {
}
