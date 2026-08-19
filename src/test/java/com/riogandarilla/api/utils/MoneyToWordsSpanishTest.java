package com.riogandarilla.api.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyToWordsSpanishTest {

    @Test
    void shouldConvertNineHundredPesos() {
        assertThat(MoneyToWordsSpanish.pesos(new BigDecimal("900.00")))
                .isEqualTo("novecientos pesos 00/100 M.N.");
    }

    @Test
    void shouldConvertAmountWithCents() {
        assertThat(MoneyToWordsSpanish.pesos(new BigDecimal("1250.50")))
                .isEqualTo("mil doscientos cincuenta pesos 50/100 M.N.");
    }
}
