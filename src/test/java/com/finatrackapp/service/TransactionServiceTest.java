package com.finatrackapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.finatrackapp.dto.request.TransactionRequest;
import com.finatrackapp.dto.response.DashboardResponse;
import com.finatrackapp.dto.response.MonthlySummaryResponse;
import com.finatrackapp.dto.response.PagedResponse;
import com.finatrackapp.dto.response.TransactionResponse;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.Category;
import com.finatrackapp.model.Transaction;
import com.finatrackapp.model.TransactionType;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.CategoryRepository;
import com.finatrackapp.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category testCategory;
    private Transaction testTransaction;
    private TransactionRequest transactionRequest;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Makanan");
        testCategory.setType(TransactionType.EXPENSE);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(new BigDecimal("50000"));
        testTransaction.setType(TransactionType.EXPENSE);
        testTransaction.setDescription("Makan siang");
        testTransaction.setDate(LocalDate.now());
        testTransaction.setCategory(testCategory);
        testTransaction.setUser(testUser);

        transactionRequest = new TransactionRequest();
        transactionRequest.setAmount(new BigDecimal("50000"));
        transactionRequest.setType(TransactionType.EXPENSE);
        transactionRequest.setDescription("Makan siang");
        transactionRequest.setDate(LocalDate.now());
        transactionRequest.setCategoryId(1L);
    }

    @Test
    @DisplayName("Create - berhasil mencatat transaksi pengeluaran")
    void createTransaction_Expense_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        TransactionResponse result =
                transactionService.createTransaction(transactionRequest, "test@example.com");

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.categoryName()).isEqualTo("Makanan");
        verify(gamificationService).updateStreakAndBadges(testUser);
    }

    @Test
    @DisplayName("Create - berhasil mencatat transaksi pemasukan")
    void createTransaction_Income_Success() {
        transactionRequest.setType(TransactionType.INCOME);
        transactionRequest.setAmount(new BigDecimal("5000000"));
        transactionRequest.setDescription("Gaji bulanan");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        TransactionResponse result =
                transactionService.createTransaction(transactionRequest, "test@example.com");

        assertThat(result.type()).isEqualTo(TransactionType.INCOME);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("5000000"));
    }

    @Test
    @DisplayName("Create - gagal karena kategori tidak ditemukan")
    void createTransaction_CategoryNotFound_ThrowsException() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transactionService.createTransaction(transactionRequest, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Kategori tidak ditemukan");
    }

    @Test
    @DisplayName("Get Paginated - berhasil dengan pagination default")
    void getTransactions_Paginated_Success() {
        Page<Transaction> page = new PageImpl<>(
                List.of(testTransaction), Pageable.ofSize(20), 1);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<TransactionResponse> result =
                transactionService.getTransactions(
                        "test@example.com", 0, 20, "date", "desc",
                        null, null, null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
    }

    @Test
    @DisplayName("Get Paginated - berhasil dengan filter tipe")
    void getTransactions_WithTypeFilter_Success() {
        Page<Transaction> page = new PageImpl<>(
                List.of(testTransaction), Pageable.ofSize(20), 1);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findByUserIdAndType(
                eq(1L), eq(TransactionType.EXPENSE), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<TransactionResponse> result =
                transactionService.getTransactions(
                        "test@example.com", 0, 20, "date", "desc",
                        TransactionType.EXPENSE, null, null);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("Get Paginated - berhasil dengan filter tanggal")
    void getTransactions_WithDateFilter_Success() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Page<Transaction> page = new PageImpl<>(
                List.of(testTransaction), Pageable.ofSize(20), 1);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findByUserIdAndDateBetween(
                eq(1L), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<TransactionResponse> result =
                transactionService.getTransactions(
                        "test@example.com", 0, 20, "date", "asc",
                        null, start, end);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("Get By ID - berhasil")
    void getTransactionById_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        TransactionResponse result =
                transactionService.getTransactionById(1L, "test@example.com");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.description()).isEqualTo("Makan siang");
    }

    @Test
    @DisplayName("Get By ID - gagal karena milik user lain")
    void getTransactionById_OtherUser_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        testTransaction.setUser(otherUser);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        assertThatThrownBy(() ->
                transactionService.getTransactionById(1L, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Update - berhasil memperbarui transaksi")
    void updateTransaction_Success() {
        transactionRequest.setAmount(new BigDecimal("75000"));
        transactionRequest.setDescription("Makan malam");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        TransactionResponse result =
                transactionService.updateTransaction(
                        1L, transactionRequest, "test@example.com");

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("75000"));
    }

    @Test
    @DisplayName("Delete - berhasil menghapus transaksi")
    void deleteTransaction_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(testTransaction));

        transactionService.deleteTransaction(1L, "test@example.com");

        verify(transactionRepository).delete(testTransaction);
    }

    @Test
    @DisplayName("Dashboard - menghitung total saldo dengan benar")
    void getDashboard_CalculatesBalanceCorrectly() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.sumAmountByUserIdAndType(1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("5000000"));
        when(transactionRepository.sumAmountByUserIdAndType(1L, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("2000000"));
        when(transactionRepository.sumAmountGroupByCategoryAndDateBetween(
                eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of());

        DashboardResponse result =
                transactionService.getDashboard("test@example.com");

        assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(result.totalExpense()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(result.balance()).isEqualByComparingTo(new BigDecimal("3000000"));
    }

    @Test
    @DisplayName("Monthly Summary - menghitung ringkasan bulanan")
    void getMonthlySummary_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("3000000"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("1500000"));
        when(transactionRepository.sumAmountGroupByCategoryAndDateBetween(
                eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of(
                        new Object[]{"Makanan", new BigDecimal("800000"), 10L},
                        new Object[]{"Transport", new BigDecimal("700000"), 5L}));

        MonthlySummaryResponse result =
                transactionService.getMonthlySummary("test@example.com", 3, 2026);

        assertThat(result.month()).isEqualTo(3);
        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.balance()).isEqualByComparingTo(new BigDecimal("1500000"));
        assertThat(result.expenseByCategory()).hasSize(2);
    }
}
