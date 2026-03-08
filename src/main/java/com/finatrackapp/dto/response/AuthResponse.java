package com.finatrackapp.dto.response;

public record AuthResponse(
    String token,
    String email,
    String fullName
) {
}
