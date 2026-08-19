package com.riogandarilla.api.controllers.web;

import com.riogandarilla.api.dto.request.CreatePaymentMovementRequest;
import com.riogandarilla.api.dto.request.CreatePaymentMovementsBatchRequest;
import com.riogandarilla.api.dto.request.GenerateMonthlyReceiptsRequest;
import com.riogandarilla.api.exception.ApiException;
import com.riogandarilla.api.services.PaymentMovementService;
import com.riogandarilla.api.services.ReceiptService;
import com.riogandarilla.api.services.ResidentService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/payment-movements")
public class PaymentMovementWebController {

    private final PaymentMovementService movementService;
    private final ReceiptService receiptService;
    private final ResidentService residentService;
    private final Validator validator;
    private final Clock clock;

    public PaymentMovementWebController(
            PaymentMovementService movementService,
            ReceiptService receiptService,
            ResidentService residentService,
            Validator validator,
            Clock clock
    ) {
        this.movementService = movementService;
        this.receiptService = receiptService;
        this.residentService = residentService;
        this.validator = validator;
        this.clock = clock;
    }

    @GetMapping
    public String page(Model model) {
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("residents", residentService.findAllActive());
        model.addAttribute("today", today);
        model.addAttribute("currentMonth", today.getMonthValue());
        model.addAttribute("currentYear", today.getYear());
        return "payment-movements";
    }

    @PostMapping("/single")
    public String createSingle(
            @RequestParam Integer casa,
            @RequestParam BigDecimal monto,
            @RequestParam(required = false) String observaciones,
            @RequestParam Integer formaPago,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPago,
            @RequestParam Integer mes,
            @RequestParam Integer anio,
            RedirectAttributes redirect
    ) {
        try {
            CreatePaymentMovementRequest request = new CreatePaymentMovementRequest(
                    casa, monto, observaciones, formaPago, fechaPago, mes, anio
            );
            validate(request);
            var created = movementService.create(request);
            redirect.addFlashAttribute("success",
                    "Movimiento " + created.folio() + " registrado para la casa " + casa);
        } catch (ApiException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/web/payment-movements";
    }

    @PostMapping("/batch")
    public String createBatch(
            @RequestParam List<Integer> casas,
            @RequestParam BigDecimal monto,
            @RequestParam(required = false) String observaciones,
            @RequestParam Integer formaPago,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPago,
            @RequestParam Integer mes,
            @RequestParam Integer anio,
            RedirectAttributes redirect
    ) {
        try {
            CreatePaymentMovementsBatchRequest request = new CreatePaymentMovementsBatchRequest(
                    casas, monto, observaciones, formaPago, fechaPago, mes, anio
            );
            validate(request);
            var created = movementService.createBatch(request);
            redirect.addFlashAttribute("success",
                    created.total() + " movimientos registrados correctamente");
        } catch (ApiException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/web/payment-movements";
    }

    @PostMapping("/monthly-receipts")
    public String monthlyReceipts(
            @RequestParam Integer mes,
            @RequestParam Integer anio,
            RedirectAttributes redirect
    ) {
        try {
            GenerateMonthlyReceiptsRequest request = new GenerateMonthlyReceiptsRequest(mes, anio);
            validate(request);
            var generated = receiptService.generateMonthly(request);
            redirect.addFlashAttribute("success",
                    generated.total() + " comprobantes guardados en " + generated.carpeta());
        } catch (ApiException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/web/payment-movements";
    }

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining(". "));
            throw new com.riogandarilla.api.exception.ValidationException("WEB_VALIDATION_ERROR", message);
        }
    }
}
