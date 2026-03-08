package com.finatrackapp.controller;

import com.finatrackapp.dto.response.ApiResponse;
import com.finatrackapp.dto.response.GamificationResponse;
import com.finatrackapp.service.GamificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<GamificationResponse>> getStatus(
            Authentication authentication) {
        GamificationResponse response =
                gamificationService.getGamificationStatus(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
