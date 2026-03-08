package com.finatrackapp.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(
    Long id,
    String name,
    BigDecimal targetAmount,
    BigDecimal currentAmount,
    double progressPercentage,
    LocalDate targetDate
) {
}
