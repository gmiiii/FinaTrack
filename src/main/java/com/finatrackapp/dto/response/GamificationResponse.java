package com.finatrackapp.dto.response;

import java.util.List;

public record GamificationResponse(
    int currentStreak,
    int longestStreak,
    List<BadgeResponse> badges
) {
}
