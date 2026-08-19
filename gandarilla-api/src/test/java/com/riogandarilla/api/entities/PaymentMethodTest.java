package com.riogandarilla.api.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodTest {

    @Test
    void shouldResolveTransferencia() {
        PaymentMethod method = PaymentMethod.fromId(1);
        assertThat(method.descripcion()).isEqualTo("Transferencia");
    }
}
