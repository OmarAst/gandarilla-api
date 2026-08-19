package com.riogandarilla.api.entities;

import java.time.OffsetDateTime;

public record Resident(
        Long id,
        String nombre,
        String telefono,
        Integer numCasa,
        boolean activo,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
