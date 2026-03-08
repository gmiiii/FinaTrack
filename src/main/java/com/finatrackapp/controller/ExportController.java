package com.finatrackapp.controller;

import com.finatrackapp.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final String EXPORT_FILENAME = "transactions.csv";

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<byte[]> exportTransactions(Authentication authentication) {
        String csv = exportService.exportTransactionsCsv(authentication.getName());
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + EXPORT_FILENAME)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
