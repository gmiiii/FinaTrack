package com.finatrackapp.dto.response;

import com.finatrackapp.model.TransactionType;

public record CategoryResponse(
    Long id,
    String name,
    TransactionType type,
    boolean isCustom
) {
}
