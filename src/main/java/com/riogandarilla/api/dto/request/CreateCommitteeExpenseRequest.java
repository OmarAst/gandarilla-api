package com.riogandarilla.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCommitteeExpenseRequest(
        @NotBlank(message = "El concepto es obligatorio")
        @Size(max = 150, message = "El concepto no puede exceder 150 caracteres")
        String concepto,
        @Size(max = 150, message = "El proveedor no puede exceder 150 caracteres")
        String proveedor,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El monto debe tener máximo 8 enteros y 2 decimales")
        BigDecimal monto,
        @NotNull(message = "La fecha del gasto es obligatoria")
        LocalDate fechaGasto,
        @NotNull @Min(1) @Max(12) Integer mes,
        @NotNull @Min(2020) @Max(2100) Integer anio,
        @NotBlank(message = "El estatus es obligatorio") String estatus
) {
}
