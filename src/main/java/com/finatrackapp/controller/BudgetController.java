package com.finatrackapp.controller;

import com.finatrackapp.dto.request.BudgetRequest;
import com.finatrackapp.dto.response.ApiResponse;
import com.finatrackapp.dto.response.BudgetResponse;
import com.finatrackapp.dto.response.BudgetStatusResponse;
import com.finatrackapp.service.BudgetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody BudgetRequest request,
            Authentication authentication) {
        BudgetResponse response =
                budgetService.createBudget(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget berhasil dibuat", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets(
            Authentication authentication,
            @RequestParam int month,
            @RequestParam int year) {
        List<BudgetResponse> response =
                budgetService.getBudgets(authentication.getName(), month, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request,
            Authentication authentication) {
        BudgetResponse response =
                budgetService.updateBudget(id, request, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Budget berhasil diperbarui", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable Long id, Authentication authentication) {
        budgetService.deleteBudget(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Budget berhasil dihapus", null));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<BudgetStatusResponse>>> getBudgetStatus(
            Authentication authentication,
            @RequestParam int month,
            @RequestParam int year) {
        List<BudgetStatusResponse> response =
                budgetService.getBudgetStatus(authentication.getName(), month, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
