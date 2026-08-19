package com.riogandarilla.api.dto.response;

public record MetaResponse(
        int statusCode,
        String status,
        String message,
        String requestId
) {
}
