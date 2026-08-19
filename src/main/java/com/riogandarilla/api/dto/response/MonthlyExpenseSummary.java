package com.riogandarilla.api.dto.response;

import java.math.BigDecimal;

public record MonthlyExpenseSummary(
        int month,
        BigDecimal covered,
        BigDecimal pending
) {
    public BigDecimal balance() {
        return covered.add(pending);
    }
}
