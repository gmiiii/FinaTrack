package com.finatrackapp.controller;

import com.finatrackapp.dto.request.SavingsGoalContributionRequest;
import com.finatrackapp.dto.request.SavingsGoalRequest;
import com.finatrackapp.dto.response.ApiResponse;
import com.finatrackapp.dto.response.SavingsGoalResponse;
import com.finatrackapp.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> createSavingsGoal(
            @Valid @RequestBody SavingsGoalRequest request,
            Authentication authentication) {
        SavingsGoalResponse response =
                savingsGoalService.createSavingsGoal(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Target tabungan berhasil dibuat", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> getSavingsGoals(
            Authentication authentication) {
        List<SavingsGoalResponse> response =
                savingsGoalService.getSavingsGoals(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> getSavingsGoalById(
            @PathVariable Long id, Authentication authentication) {
        SavingsGoalResponse response =
                savingsGoalService.getSavingsGoalById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> updateSavingsGoal(
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalRequest request,
            Authentication authentication) {
        SavingsGoalResponse response =
                savingsGoalService.updateSavingsGoal(
                        id, request, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Target tabungan berhasil diperbarui", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSavingsGoal(
            @PathVariable Long id, Authentication authentication) {
        savingsGoalService.deleteSavingsGoal(id, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Target tabungan berhasil dihapus", null));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> contribute(
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalContributionRequest request,
            Authentication authentication) {
        SavingsGoalResponse response =
                savingsGoalService.contribute(id, request, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Kontribusi berhasil ditambahkan", response));
    }
}
