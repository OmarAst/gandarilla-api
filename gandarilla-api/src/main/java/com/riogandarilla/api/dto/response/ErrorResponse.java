package com.riogandarilla.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        String path,
        String code,
        String error,
        List<String> details,
        String requestId,
        OffsetDateTime timestamp
) {
}
