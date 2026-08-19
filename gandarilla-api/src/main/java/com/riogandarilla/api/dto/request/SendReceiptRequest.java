package com.riogandarilla.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SendReceiptRequest(
        @NotNull(message = "La casa es obligatoria")
        @Min(value = 1, message = "La casa debe estar entre 1 y 50")
        @Max(value = 50, message = "La casa debe estar entre 1 y 50")
        Integer casa,

        @NotNull(message = "El mes es obligatorio")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12")
        Integer mes
) {
}
