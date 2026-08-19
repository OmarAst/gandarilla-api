package com.riogandarilla.api.services;

import com.riogandarilla.api.dto.request.SendReceiptRequest;
import com.riogandarilla.api.dto.request.GenerateMonthlyReceiptsRequest;
import com.riogandarilla.api.dto.response.MonthlyReceiptsResponse;
import com.riogandarilla.api.dto.response.SendReceiptResponse;

public interface ReceiptService {
    SendReceiptResponse generateAndSend(long movementId);

    SendReceiptResponse generateAndSend(SendReceiptRequest request);

    MonthlyReceiptsResponse generateMonthly(GenerateMonthlyReceiptsRequest request);
}
