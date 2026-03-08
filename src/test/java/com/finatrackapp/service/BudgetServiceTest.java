package com.finatrackapp.service;

import java.math.BigDecimal;
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

import com.finatrackapp.dto.request.BudgetRequest;
import com.finatrackapp.dto.response.BudgetResponse;
import com.finatrackapp.dto.response.BudgetStatusResponse;
import com.finatrackapp.exception.DuplicateResourceException;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.Budget;
import com.finatrackapp.model.Category;
import com.finatrackapp.model.TransactionType;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.BudgetRepository;
import com.finatrackapp.repository.CategoryRepository;
import com.finatrackapp.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private BudgetRequest budgetRequest;

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

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setCategory(testCategory);
        testBudget.setUser(testUser);
        testBudget.setMonthlyLimit(new BigDecimal("1000000"));
        testBudget.setMonth(3);
        testBudget.setYear(2026);

        budgetRequest = new BudgetRequest();
        budgetRequest.setCategoryId(1L);
        budgetRequest.setMonthlyLimit(new BigDecimal("1000000"));
        budgetRequest.setMonth(3);
        budgetRequest.setYear(2026);
    }

    @Test
    @DisplayName("Create - berhasil membuat budget")
    void createBudget_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(1L, 1L, 3, 2026))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        BudgetResponse result =
                budgetService.createBudget(budgetRequest, "test@example.com");

        assertThat(result.categoryName()).isEqualTo("Makanan");
        assertThat(result.monthlyLimit())
                .isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    @DisplayName("Create - gagal karena budget duplikat")
    void createBudget_Duplicate_ThrowsException() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(1L, 1L, 3, 2026))
                .thenReturn(Optional.of(testBudget));

        assertThatThrownBy(() ->
                budgetService.createBudget(budgetRequest, "test@example.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Create - gagal karena kategori tidak ditemukan")
    void createBudget_CategoryNotFound_ThrowsException() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                budgetService.createBudget(budgetRequest, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get Budgets - berhasil mendapatkan daftar budget")
    void getBudgets_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(budgetRepository.findByUserIdAndMonthAndYear(1L, 3, 2026))
                .thenReturn(List.of(testBudget));

        List<BudgetResponse> result =
                budgetService.getBudgets("test@example.com", 3, 2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).month()).isEqualTo(3);
    }

    @Test
    @DisplayName("Update - berhasil memperbarui budget")
    void updateBudget_Success() {
        budgetRequest.setMonthlyLimit(new BigDecimal("1500000"));

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        BudgetResponse result =
                budgetService.updateBudget(1L, budgetRequest, "test@example.com");

        assertThat(result.monthlyLimit())
                .isEqualByComparingTo(new BigDecimal("1500000"));
    }

    @Test
    @DisplayName("Update - gagal karena budget milik user lain")
    void updateBudget_OtherUser_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        testBudget.setUser(otherUser);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        assertThatThrownBy(() ->
                budgetService.updateBudget(1L, budgetRequest, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Delete - berhasil menghapus budget")
    void deleteBudget_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        budgetService.deleteBudget(1L, "test@example.com");

        verify(budgetRepository).delete(testBudget);
    }

    @Test
    @DisplayName("Budget Status - SAFE ketika pengeluaran rendah")
    void getBudgetStatus_Safe() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(budgetRepository.findByUserIdAndMonthAndYear(1L, 3, 2026))
                .thenReturn(List.of(testBudget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                eq(1L), eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("300000"));

        List<BudgetStatusResponse> result =
                budgetService.getBudgetStatus("test@example.com", 3, 2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("SAFE");
        assertThat(result.get(0).percentageUsed()).isLessThan(80.0);
    }

    @Test
    @DisplayName("Budget Status - WARNING ketika pengeluaran mendekati limit")
    void getBudgetStatus_Warning() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(budgetRepository.findByUserIdAndMonthAndYear(1L, 3, 2026))
                .thenReturn(List.of(testBudget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                eq(1L), eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("850000"));

        List<BudgetStatusResponse> result =
                budgetService.getBudgetStatus("test@example.com", 3, 2026);

        assertThat(result.get(0).status()).isEqualTo("WARNING");
        assertThat(result.get(0).percentageUsed()).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    @DisplayName("Budget Status - EXCEEDED ketika melebihi limit")
    void getBudgetStatus_Exceeded() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(budgetRepository.findByUserIdAndMonthAndYear(1L, 3, 2026))
                .thenReturn(List.of(testBudget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                eq(1L), eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("1200000"));

        List<BudgetStatusResponse> result =
                budgetService.getBudgetStatus("test@example.com", 3, 2026);

        assertThat(result.get(0).status()).isEqualTo("EXCEEDED");
        assertThat(result.get(0).remaining()).isNegative();
    }
}
