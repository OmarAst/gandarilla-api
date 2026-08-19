package com.riogandarilla.api.controllers.web;

import com.riogandarilla.api.dto.request.CreateCommitteeExpenseRequest;
import com.riogandarilla.api.services.CommitteeExpenseService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/payment-movements")
public class CommitteeExpenseWebController {
    private final CommitteeExpenseService expenseService;
    private final Validator validator;

    public CommitteeExpenseWebController(CommitteeExpenseService expenseService, Validator validator) {
        this.expenseService = expenseService;
        this.validator = validator;
    }

    @PostMapping("/expense")
    public String create(
            @RequestParam String concepto,
            @RequestParam(required = false) String proveedor,
            @RequestParam BigDecimal monto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaGasto,
            @RequestParam Integer mes,
            @RequestParam Integer anio,
            @RequestParam String estatus,
            RedirectAttributes redirect
    ) {
        try {
            CreateCommitteeExpenseRequest request = new CreateCommitteeExpenseRequest(
                    concepto, proveedor, monto, fechaGasto, mes, anio, estatus
            );
            Set<ConstraintViolation<CreateCommitteeExpenseRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .distinct()
                        .collect(Collectors.joining(". ")));
            }
            expenseService.create(request);
            redirect.addFlashAttribute("success", "Gasto registrado correctamente");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/web/payment-movements";
    }
}
