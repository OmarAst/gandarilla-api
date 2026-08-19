package com.riogandarilla.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePaymentMovementsBatchRequest(
        @NotNull(message = "La lista de casas es obligatoria")
        @Size(min = 1, max = 50, message = "Debes enviar entre 1 y 50 casas")
        List<@NotNull(message = "La casa no puede ser nula")
                @Min(value = 1, message = "El número de casa debe estar entre 1 y 50")
                @Max(value = 50, message = "El número de casa debe estar entre 1 y 50") Integer> casas,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El monto debe tener máximo 8 enteros y 2 decimales")
        BigDecimal monto,

        @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
        String observaciones,

        @NotNull(message = "La forma de pago es obligatoria")
        @Min(value = 1, message = "La forma de pago debe estar entre 1 y 4")
        @Max(value = 4, message = "La forma de pago debe estar entre 1 y 4")
        Integer formaPago,

        @NotNull(message = "La fecha de pago es obligatoria")
        @PastOrPresent(message = "La fecha de pago no puede estar en el futuro")
        LocalDate fechaPago,

        @NotNull(message = "El mes es obligatorio")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12")
        Integer mes,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 2020, message = "El año debe ser mayor o igual a 2020")
        @Max(value = 2100, message = "El año debe ser menor o igual a 2100")
        Integer anio
) {
    public CreatePaymentMovementRequest forHouse(Integer house) {
        return new CreatePaymentMovementRequest(
                house, monto, observaciones, formaPago, fechaPago, mes, anio
        );
    }
}
