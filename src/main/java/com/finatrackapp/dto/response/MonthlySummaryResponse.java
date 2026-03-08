package com.finatrackapp.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(
    int month,
    int year,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal balance,
    List<CategorySummaryResponse> expenseByCategory
) {
}
