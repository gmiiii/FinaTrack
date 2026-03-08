package com.finatrackapp.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String fullName,
    String email,
    LocalDateTime createdAt
) {
}
