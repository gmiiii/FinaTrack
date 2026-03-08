package com.finatrackapp.dto.response;

import java.time.LocalDateTime;

public record BadgeResponse(
    String name,
    String displayName,
    String description,
    LocalDateTime awardedAt
) {
}
