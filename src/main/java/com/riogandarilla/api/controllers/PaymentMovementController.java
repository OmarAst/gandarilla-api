package com.riogandarilla.api.controllers;

import com.riogandarilla.api.dto.request.CreatePaymentMovementRequest;
import com.riogandarilla.api.dto.request.CreatePaymentMovementsBatchRequest;
import com.riogandarilla.api.dto.response.ApiResponse;
import com.riogandarilla.api.dto.response.PaymentMovementResponse;
import com.riogandarilla.api.dto.response.PaymentMovementsBatchResponse;
import com.riogandarilla.api.dto.response.SendReceiptResponse;
import com.riogandarilla.api.services.PaymentMovementService;
import com.riogandarilla.api.services.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@Validated
@RestController
@RequestMapping("/api/payment-movements")
@SecurityRequirement(name = "bearerAuth")
public class PaymentMovementController {

    private final PaymentMovementService movementService;
    private final ReceiptService receiptService;

    public PaymentMovementController(
            PaymentMovementService movementService,
            ReceiptService receiptService
    ) {
        this.movementService = movementService;
        this.receiptService = receiptService;
    }

    @PostMapping
    @Operation(summary = "Registra un movimiento de pago")
    public ResponseEntity<ApiResponse<PaymentMovementResponse>> create(
            @Valid @RequestBody CreatePaymentMovementRequest request
    ) {
        log.info(
                "Solicitud para registrar pago house={} period={}/{}",
                request.casa(),
                request.mes(),
                request.anio()
        );
        PaymentMovementResponse movement = movementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        "Movimiento de pago registrado correctamente",
                        movement
                ));
    }

    @PostMapping("/batch")
    @Operation(summary = "Registra el mismo pago para varias casas en una sola transacción")
    public ResponseEntity<ApiResponse<PaymentMovementsBatchResponse>> createBatch(
            @Valid @RequestBody CreatePaymentMovementsBatchRequest request
    ) {
        log.info("Solicitud de lote de pagos housesCount={} period={}/{}",
                request.casas().size(), request.mes(), request.anio());
        PaymentMovementsBatchResponse movements = movementService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        "Movimientos de pago registrados correctamente",
                        movements
                ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un movimiento de pago")
    public ApiResponse<PaymentMovementResponse> findById(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") long id
    ) {
        return ApiResponse.success(movementService.findById(id));
    }

    @PostMapping("/{id}/send-receipt")
    @Operation(summary = "Genera y envía por WhatsApp el recibo de un movimiento")
    public ResponseEntity<ApiResponse<SendReceiptResponse>> sendReceipt(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") long id
    ) {
        SendReceiptResponse response = receiptService.generateAndSend(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Comprobante generado y enviado correctamente",
                        response
                ));
    }
}
