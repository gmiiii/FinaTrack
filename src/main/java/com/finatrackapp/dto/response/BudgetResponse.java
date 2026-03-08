package com.finatrackapp.dto.response;

import java.math.BigDecimal;

public record BudgetResponse(
    Long id,
    String categoryName,
    Long categoryId,
    BigDecimal monthlyLimit,
    int month,
    int year
) {
}
