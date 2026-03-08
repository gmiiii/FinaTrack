package com.finatrackapp.dto.request;

import com.finatrackapp.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryRequest {

    @NotBlank(message = "Nama kategori wajib diisi")
    private String name;

    @NotNull(message = "Tipe kategori wajib diisi (INCOME/EXPENSE)")
    private TransactionType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
