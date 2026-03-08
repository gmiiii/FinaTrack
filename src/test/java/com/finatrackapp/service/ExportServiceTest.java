package com.finatrackapp.service;

import com.finatrackapp.model.Category;
import com.finatrackapp.model.Transaction;
import com.finatrackapp.model.TransactionType;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ExportService exportService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Export CSV - data kosong, hanya header")
    void exportCsv_NoData_ReturnsHeaderOnly() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of());

        String csv = exportService.exportTransactionsCsv("test@example.com");

        assertThat(csv).startsWith("Tanggal,Tipe,Kategori,Nominal,Deskripsi");
        String[] lines = csv.trim().split("\\r?\\n");
        assertThat(lines).hasSize(1);
    }

    @Test
    @DisplayName("Export CSV - data ada, format benar")
    void exportCsv_WithData_ReturnsValidCsv() {
        Category category = new Category();
        category.setName("Makanan");

        Transaction tx1 = new Transaction();
        tx1.setDate(LocalDate.of(2026, 3, 1));
        tx1.setType(TransactionType.EXPENSE);
        tx1.setCategory(category);
        tx1.setAmount(new BigDecimal("50000"));
        tx1.setDescription("Makan siang");

        Transaction tx2 = new Transaction();
        tx2.setDate(LocalDate.of(2026, 3, 2));
        tx2.setType(TransactionType.INCOME);
        tx2.setCategory(category);
        tx2.setAmount(new BigDecimal("5000000"));
        tx2.setDescription(null);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of(tx1, tx2));

        String csv = exportService.exportTransactionsCsv("test@example.com");

        String[] lines = csv.trim().split("\\r?\\n");
        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("Tanggal,Tipe,Kategori,Nominal,Deskripsi");
        assertThat(lines[1]).contains("2026-03-01");
        assertThat(lines[1]).contains("EXPENSE");
        assertThat(lines[1]).contains("50000");
        assertThat(lines[2]).contains("INCOME");
        assertThat(lines[2]).contains("5000000");
    }

    @Test
    @DisplayName("Export CSV - karakter khusus di-escape dengan benar")
    void exportCsv_WithSpecialCharacters_EscapesCorrectly() {
        Category category = new Category();
        category.setName("Makan, Minum");

        Transaction tx = new Transaction();
        tx.setDate(LocalDate.of(2026, 3, 1));
        tx.setType(TransactionType.EXPENSE);
        tx.setCategory(category);
        tx.setAmount(new BigDecimal("25000"));
        tx.setDescription("Nasi \"goreng\" special");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of(tx));

        String csv = exportService.exportTransactionsCsv("test@example.com");

        assertThat(csv).contains("\"Makan, Minum\"");
        assertThat(csv).contains("\"Nasi \"\"goreng\"\" special\"");
    }

    @Test
    @DisplayName("escapeCsv - nilai tanpa karakter khusus tidak di-escape")
    void escapeCsv_NoSpecialChars_ReturnsOriginal() {
        String result = exportService.escapeCsv("Hello World");
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("escapeCsv - nilai dengan koma di-escape")
    void escapeCsv_WithComma_EscapesCorrectly() {
        String result = exportService.escapeCsv("Hello, World");
        assertThat(result).isEqualTo("\"Hello, World\"");
    }

    @Test
    @DisplayName("escapeCsv - nilai dengan kutip ganda di-escape")
    void escapeCsv_WithQuotes_EscapesCorrectly() {
        String result = exportService.escapeCsv("Hello \"World\"");
        assertThat(result).isEqualTo("\"Hello \"\"World\"\"\"");
    }

    @Test
    @DisplayName("escapeCsv - nilai dengan newline di-escape")
    void escapeCsv_WithNewline_EscapesCorrectly() {
        String result = exportService.escapeCsv("Hello\nWorld");
        assertThat(result).isEqualTo("\"Hello\nWorld\"");
    }
}
