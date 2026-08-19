package com.riogandarilla.api.services;

import com.riogandarilla.api.dto.request.CreatePaymentMovementRequest;
import com.riogandarilla.api.dto.request.CreatePaymentMovementsBatchRequest;
import com.riogandarilla.api.dto.response.PaymentMovementResponse;
import com.riogandarilla.api.dto.response.PaymentMovementsBatchResponse;

public interface PaymentMovementService {
    PaymentMovementResponse create(CreatePaymentMovementRequest request);

    PaymentMovementsBatchResponse createBatch(CreatePaymentMovementsBatchRequest request);

    PaymentMovementResponse findById(long id);
}
