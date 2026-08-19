package com.riogandarilla.api.dto.response;

import java.util.List;

public record PaymentMovementsBatchResponse(
        int total,
        List<PaymentMovementResponse> movimientos
) {
}
