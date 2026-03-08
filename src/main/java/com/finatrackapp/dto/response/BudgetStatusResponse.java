package com.finatrackapp.dto.response;

import java.math.BigDecimal;

public record BudgetStatusResponse(
    Long budgetId,
    String categoryName,
    BigDecimal monthlyLimit,
    BigDecimal amountSpent,
    BigDecimal remaining,
    double percentageUsed,
    String status
) {
}
