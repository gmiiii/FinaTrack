package com.finatrackapp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SavingsGoalContributionRequest {

    @NotNull(message = "Jumlah kontribusi wajib diisi")
    @DecimalMin(value = "0.01", message = "Jumlah kontribusi harus lebih dari 0")
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
