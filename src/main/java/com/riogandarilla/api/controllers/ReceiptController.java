package com.riogandarilla.api.controllers;

import com.riogandarilla.api.dto.request.SendReceiptRequest;
import com.riogandarilla.api.dto.request.GenerateMonthlyReceiptsRequest;
import com.riogandarilla.api.dto.response.ApiResponse;
import com.riogandarilla.api.dto.response.MonthlyReceiptsResponse;
import com.riogandarilla.api.dto.response.SendReceiptResponse;
import com.riogandarilla.api.services.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping("/send")
    @Operation(summary = "Busca el pago más reciente de la casa y mes del año actual, genera el recibo y lo envía")
    public ResponseEntity<ApiResponse<SendReceiptResponse>> send(
            @Valid @RequestBody SendReceiptRequest request
    ) {
        SendReceiptResponse response = receiptService.generateAndSend(request);
        return ResponseEntity.ok(
                ApiResponse.success("Comprobante generado y enviado correctamente", response));
    }

    @PostMapping("/generate-monthly")
    @Operation(summary = "Genera y guarda los comprobantes de residentes con pago registrado en el periodo")
    public ResponseEntity<ApiResponse<MonthlyReceiptsResponse>> generateMonthly(
            @Valid @RequestBody GenerateMonthlyReceiptsRequest request
    ) {
        MonthlyReceiptsResponse response = receiptService.generateMonthly(request);
        return ResponseEntity.ok(
                ApiResponse.success("Comprobantes mensuales generados correctamente", response));
    }
}
