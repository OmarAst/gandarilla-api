package com.riogandarilla.api.entities;

import com.riogandarilla.api.exception.BusinessException;

import java.util.Arrays;

public enum PaymentMethod {
    TRANSFERENCIA(1, "Transferencia"),
    DEPOSITO(2, "Depósito"),
    EFECTIVO(3, "Efectivo"),
    OTRO(4, "Otro");

    private final int id;
    private final String descripcion;

    PaymentMethod(int id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public int id() {
        return id;
    }

    public String descripcion() {
        return descripcion;
    }

    public static PaymentMethod fromId(Integer id) {
        if (id == null) {
            throw new BusinessException(
                    "PAYMENT_METHOD_REQUIRED",
                    "La forma de pago es obligatoria"
            );
        }
        return Arrays.stream(values())
                .filter(value -> value.id == id)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "PAYMENT_METHOD_INVALID",
                        "La forma de pago debe estar entre 1 y 4"
                ));
    }
}
