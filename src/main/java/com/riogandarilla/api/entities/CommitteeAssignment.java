package com.riogandarilla.api.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CommitteeAssignment(
        Long id,
        Integer numCasa,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
