package com.riogandarilla.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateMonthlyReceiptsRequest(
        @NotNull(message = "El mes es obligatorio")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12")
        Integer mes,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 2020, message = "El año debe ser mayor o igual a 2020")
        @Max(value = 2100, message = "El año debe ser menor o igual a 2100")
        Integer anio
) {
}
