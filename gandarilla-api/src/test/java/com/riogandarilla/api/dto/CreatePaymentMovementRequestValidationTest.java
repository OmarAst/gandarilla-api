package com.riogandarilla.api.dto;

import com.riogandarilla.api.dto.request.CreatePaymentMovementRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePaymentMovementRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldRejectInvalidPaymentData(CreatePaymentMovementRequest request) {
        assertThat(validator.validate(request)).isNotEmpty();
    }

    static Stream<CreatePaymentMovementRequest> invalidRequests() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                request(0, new BigDecimal("800"), 1, 8, today),
                request(51, new BigDecimal("800"), 1, 8, today),
                request(1, BigDecimal.ZERO, 1, 8, today),
                request(1, new BigDecimal("-1"), 1, 8, today),
                request(1, new BigDecimal("800"), 0, 8, today),
                request(1, new BigDecimal("800"), 5, 8, today),
                request(1, new BigDecimal("800"), 1, 0, today),
                request(1, new BigDecimal("800"), 1, 13, today)
        );
    }

    private static CreatePaymentMovementRequest request(
            int house, BigDecimal amount, int method, int month, LocalDate date
    ) {
        return new CreatePaymentMovementRequest(house, amount, null, method, date, month, 2026);
    }
}
