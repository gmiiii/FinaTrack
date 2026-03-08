package com.finatrackapp.dto.request;

import com.finatrackapp.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotNull(message = "Nominal wajib diisi")
    @DecimalMin(value = "0.01", message = "Nominal harus lebih dari 0")
    private BigDecimal amount;

    @NotNull(message = "Tipe transaksi wajib diisi (INCOME/EXPENSE)")
    private TransactionType type;

    private String description;

    @NotNull(message = "Tanggal wajib diisi")
    private LocalDate date;

    @NotNull(message = "Kategori wajib diisi")
    private Long categoryId;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
