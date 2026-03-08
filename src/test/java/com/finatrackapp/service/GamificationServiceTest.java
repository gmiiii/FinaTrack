package com.finatrackapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finatrackapp.dto.response.GamificationResponse;
import com.finatrackapp.model.BadgeType;
import com.finatrackapp.model.Budget;
import com.finatrackapp.model.Category;
import com.finatrackapp.model.TransactionType;
import com.finatrackapp.model.User;
import com.finatrackapp.model.UserBadge;
import com.finatrackapp.repository.BudgetRepository;
import com.finatrackapp.repository.TransactionRepository;
import com.finatrackapp.repository.UserBadgeRepository;
import com.finatrackapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class GamificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private GamificationService gamificationService;

    private User testUser;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setCurrentStreak(0);
        testUser.setLongestStreak(0);
        testUser.setLastTransactionDate(null);
    }

    @Test
    @DisplayName("Streak - transaksi pertama, streak jadi 1")
    void updateStreak_FirstTransaction_SetsStreakToOne() {
        gamificationService.updateStreak(testUser);

        assertThat(testUser.getCurrentStreak()).isEqualTo(1);
        assertThat(testUser.getLongestStreak()).isEqualTo(1);
        assertThat(testUser.getLastTransactionDate()).isEqualTo(LocalDate.now());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Streak - hari berturut-turut, streak bertambah")
    void updateStreak_ConsecutiveDay_IncrementsStreak() {
        testUser.setCurrentStreak(5);
        testUser.setLongestStreak(5);
        testUser.setLastTransactionDate(LocalDate.now().minusDays(1));

        gamificationService.updateStreak(testUser);

        assertThat(testUser.getCurrentStreak()).isEqualTo(6);
        assertThat(testUser.getLongestStreak()).isEqualTo(6);
    }

    @Test
    @DisplayName("Streak - hari sama, streak tidak berubah")
    void updateStreak_SameDay_NoChange() {
        testUser.setCurrentStreak(5);
        testUser.setLongestStreak(10);
        testUser.setLastTransactionDate(LocalDate.now());

        gamificationService.updateStreak(testUser);

        assertThat(testUser.getCurrentStreak()).isEqualTo(5);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Streak - gap lebih dari 1 hari, streak reset ke 1")
    void updateStreak_GapDay_ResetsStreak() {
        testUser.setCurrentStreak(10);
        testUser.setLongestStreak(10);
        testUser.setLastTransactionDate(LocalDate.now().minusDays(3));

        gamificationService.updateStreak(testUser);

        assertThat(testUser.getCurrentStreak()).isEqualTo(1);
        assertThat(testUser.getLongestStreak()).isEqualTo(10);
    }

    @Test
    @DisplayName("Streak - longest streak diperbarui jika current lebih tinggi")
    void updateStreak_UpdatesLongestStreak() {
        testUser.setCurrentStreak(14);
        testUser.setLongestStreak(14);
        testUser.setLastTransactionDate(LocalDate.now().minusDays(1));

        gamificationService.updateStreak(testUser);

        assertThat(testUser.getCurrentStreak()).isEqualTo(15);
        assertThat(testUser.getLongestStreak()).isEqualTo(15);
    }

    @Test
    @DisplayName("Badge - FIRST_TRANSACTION diberikan saat transaksi pertama")
    void updateStreakAndBadges_FirstTransaction_AwardsBadge() {
        when(transactionRepository.countByUserId(1L)).thenReturn(1L);
        when(userBadgeRepository.existsByUserIdAndBadgeType(
                1L, BadgeType.FIRST_TRANSACTION)).thenReturn(false);

        gamificationService.updateStreakAndBadges(testUser);

        verify(userBadgeRepository).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("Badge - WEEK_STREAK diberikan saat streak >= 7")
    void updateStreakAndBadges_WeekStreak_AwardsBadge() {
        testUser.setCurrentStreak(6);
        testUser.setLongestStreak(6);
        testUser.setLastTransactionDate(LocalDate.now().minusDays(1));

        when(transactionRepository.countByUserId(1L)).thenReturn(50L);
        when(userBadgeRepository.existsByUserIdAndBadgeType(
                1L, BadgeType.WEEK_STREAK)).thenReturn(false);

        gamificationService.updateStreakAndBadges(testUser);

        assertThat(testUser.getCurrentStreak()).isEqualTo(7);
    }

    @Test
    @DisplayName("Badge - CENTURY_TRANSACTIONS saat mencapai 100 transaksi")
    void updateStreakAndBadges_CenturyTransactions_AwardsBadge() {
        testUser.setLastTransactionDate(LocalDate.now());

        when(transactionRepository.countByUserId(1L)).thenReturn(100L);
        when(userBadgeRepository.existsByUserIdAndBadgeType(
                1L, BadgeType.CENTURY_TRANSACTIONS)).thenReturn(false);

        gamificationService.updateStreakAndBadges(testUser);

        verify(userBadgeRepository).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("Gamification Status - mengembalikan data streak dan badges")
    void getGamificationStatus_Success() {
        testUser.setCurrentStreak(5);
        testUser.setLongestStreak(10);

        UserBadge badge = new UserBadge();
        badge.setBadgeType(BadgeType.FIRST_TRANSACTION);
        badge.setUser(testUser);
        badge.setAwardedAt(LocalDateTime.now());

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(userBadgeRepository.existsByUserIdAndBadgeType(
                1L, BadgeType.BUDGET_MASTER)).thenReturn(false);
        when(budgetRepository.findByUserIdAndMonthAndYear(
                eq(1L), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of());
        when(userBadgeRepository.findByUserId(1L)).thenReturn(List.of(badge));

        GamificationResponse result =
                gamificationService.getGamificationStatus("test@example.com");

        assertThat(result.currentStreak()).isEqualTo(5);
        assertThat(result.longestStreak()).isEqualTo(10);
        assertThat(result.badges()).hasSize(1);
        assertThat(result.badges().get(0).displayName()).isEqualTo("Pencatat Pertama");
    }

    @Test
    @DisplayName("Budget Master - badge diberikan jika 3 bulan berturut-turut di bawah budget")
    void getGamificationStatus_BudgetMaster_AwardsBadge() {
        testUser.setCurrentStreak(5);
        testUser.setLongestStreak(10);

        Category category = new Category();
        category.setId(1L);
        category.setName("Makanan");

        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setMonthlyLimit(new BigDecimal("1000000"));

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(userBadgeRepository.existsByUserIdAndBadgeType(
                1L, BadgeType.BUDGET_MASTER)).thenReturn(false);
        when(budgetRepository.findByUserIdAndMonthAndYear(
                eq(1L), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(budget));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                eq(1L), eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("500000"));
        when(userBadgeRepository.findByUserId(1L)).thenReturn(List.of());

        GamificationResponse result =
                gamificationService.getGamificationStatus("test@example.com");

        verify(userBadgeRepository).save(any(UserBadge.class));
        assertThat(result).isNotNull();
    }
}
