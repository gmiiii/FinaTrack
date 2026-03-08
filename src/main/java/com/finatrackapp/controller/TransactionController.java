package com.finatrackapp.controller;

import com.finatrackapp.dto.request.TransactionRequest;
import com.finatrackapp.dto.response.ApiResponse;
import com.finatrackapp.dto.response.DashboardResponse;
import com.finatrackapp.dto.response.MonthlySummaryResponse;
import com.finatrackapp.dto.response.PagedResponse;
import com.finatrackapp.dto.response.TransactionResponse;
import com.finatrackapp.model.TransactionType;
import com.finatrackapp.service.TransactionService;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final String DEFAULT_SORT_FIELD = "date";
    private static final String DEFAULT_SORT_DIR = "desc";

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT_FIELD) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIR) String sortDir,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        PagedResponse<TransactionResponse> response =
                transactionService.getTransactions(
                        authentication.getName(), page, size,
                        sortBy, sortDir, type, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable Long id, Authentication authentication) {
        TransactionResponse response =
                transactionService.getTransactionById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {
        TransactionResponse response =
                transactionService.createTransaction(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaksi berhasil ditambahkan", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {
        TransactionResponse response =
                transactionService.updateTransaction(
                        id, request, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Transaksi berhasil diperbarui", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable Long id, Authentication authentication) {
        transactionService.deleteTransaction(id, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Transaksi berhasil dihapus", null));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            Authentication authentication) {
        DashboardResponse response =
                transactionService.getDashboard(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthlySummary(
            Authentication authentication,
            @RequestParam int month,
            @RequestParam int year) {
        MonthlySummaryResponse response =
                transactionService.getMonthlySummary(
                        authentication.getName(), month, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
