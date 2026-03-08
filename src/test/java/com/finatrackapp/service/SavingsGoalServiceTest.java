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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finatrackapp.dto.request.SavingsGoalContributionRequest;
import com.finatrackapp.dto.request.SavingsGoalRequest;
import com.finatrackapp.dto.response.SavingsGoalResponse;
import com.finatrackapp.exception.InvalidOperationException;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.BadgeType;
import com.finatrackapp.model.SavingsGoal;
import com.finatrackapp.model.User;
import com.finatrackapp.model.UserBadge;
import com.finatrackapp.repository.SavingsGoalRepository;
import com.finatrackapp.repository.UserBadgeRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private User testUser;
    private SavingsGoal testGoal;
    private SavingsGoalRequest goalRequest;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testGoal = new SavingsGoal();
        testGoal.setId(1L);
        testGoal.setName("Beli Laptop");
        testGoal.setTargetAmount(new BigDecimal("10000000"));
        testGoal.setCurrentAmount(new BigDecimal("3000000"));
        testGoal.setTargetDate(LocalDate.of(2026, 12, 31));
        testGoal.setUser(testUser);

        goalRequest = new SavingsGoalRequest();
        goalRequest.setName("Dana Darurat");
        goalRequest.setTargetAmount(new BigDecimal("20000000"));
        goalRequest.setTargetDate(LocalDate.of(2027, 6, 30));
    }

    @Test
    @DisplayName("Create - berhasil membuat target tabungan")
    void createSavingsGoal_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.save(any(SavingsGoal.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(userBadgeRepository.existsByUserIdAndBadgeType(1L, BadgeType.SAVINGS_STARTER))
                .thenReturn(false);

        SavingsGoalResponse result =
                savingsGoalService.createSavingsGoal(goalRequest, "test@example.com");

        assertThat(result.name()).isEqualTo("Dana Darurat");
        assertThat(result.currentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.progressPercentage()).isZero();
        verify(userBadgeRepository).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("Create - tidak memberikan badge SAVINGS_STARTER jika sudah punya")
    void createSavingsGoal_BadgeAlreadyExists() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.save(any(SavingsGoal.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(userBadgeRepository.existsByUserIdAndBadgeType(1L, BadgeType.SAVINGS_STARTER))
                .thenReturn(true);

        savingsGoalService.createSavingsGoal(goalRequest, "test@example.com");

        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("Get All - berhasil mendapatkan daftar target")
    void getSavingsGoals_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findByUserId(1L)).thenReturn(List.of(testGoal));

        List<SavingsGoalResponse> result =
                savingsGoalService.getSavingsGoals("test@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Beli Laptop");
        assertThat(result.get(0).progressPercentage()).isEqualTo(30.00);
    }

    @Test
    @DisplayName("Get By ID - berhasil")
    void getSavingsGoalById_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

        SavingsGoalResponse result =
                savingsGoalService.getSavingsGoalById(1L, "test@example.com");

        assertThat(result.name()).isEqualTo("Beli Laptop");
    }

    @Test
    @DisplayName("Get By ID - gagal karena milik user lain")
    void getSavingsGoalById_OtherUser_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        testGoal.setUser(otherUser);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

        assertThatThrownBy(() ->
                savingsGoalService.getSavingsGoalById(1L, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Update - berhasil memperbarui target")
    void updateSavingsGoal_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));
        when(savingsGoalRepository.save(any(SavingsGoal.class)))
                .thenAnswer(i -> i.getArgument(0));

        SavingsGoalResponse result =
                savingsGoalService.updateSavingsGoal(
                        1L, goalRequest, "test@example.com");

        assertThat(result.name()).isEqualTo("Dana Darurat");
        assertThat(result.targetAmount())
                .isEqualByComparingTo(new BigDecimal("20000000"));
    }

    @Test
    @DisplayName("Delete - berhasil menghapus target")
    void deleteSavingsGoal_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

        savingsGoalService.deleteSavingsGoal(1L, "test@example.com");

        verify(savingsGoalRepository).delete(testGoal);
    }

    @Test
    @DisplayName("Contribute - berhasil menambah kontribusi")
    void contribute_Success() {
        SavingsGoalContributionRequest contribution =
                new SavingsGoalContributionRequest();
        contribution.setAmount(new BigDecimal("2000000"));

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));
        when(savingsGoalRepository.save(any(SavingsGoal.class)))
                .thenAnswer(i -> i.getArgument(0));

        SavingsGoalResponse result =
                savingsGoalService.contribute(1L, contribution, "test@example.com");

        assertThat(result.currentAmount())
                .isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(result.progressPercentage()).isEqualTo(50.00);
    }

    @Test
    @DisplayName("Contribute - gagal karena melebihi target")
    void contribute_ExceedsTarget_ThrowsException() {
        SavingsGoalContributionRequest contribution =
                new SavingsGoalContributionRequest();
        contribution.setAmount(new BigDecimal("8000000"));

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

        assertThatThrownBy(() ->
                savingsGoalService.contribute(1L, contribution, "test@example.com"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Kontribusi melebihi target tabungan");
    }

    @Test
    @DisplayName("Contribute - award badge ketika target tercapai 100%")
    void contribute_ReachesTarget_AwardsBadge() {
        testGoal.setCurrentAmount(new BigDecimal("9500000"));

        SavingsGoalContributionRequest contribution =
                new SavingsGoalContributionRequest();
        contribution.setAmount(new BigDecimal("500000"));

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));
        when(savingsGoalRepository.save(any(SavingsGoal.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(userBadgeRepository.existsByUserIdAndBadgeType(
                1L, BadgeType.SAVINGS_ACHIEVER)).thenReturn(false);

        SavingsGoalResponse result =
                savingsGoalService.contribute(1L, contribution, "test@example.com");

        assertThat(result.progressPercentage()).isEqualTo(100.00);
        verify(userBadgeRepository).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("Progress - kalkulasi persentase 0 ketika target amount 0")
    void toResponse_ZeroTarget_ZeroProgress() {
        testGoal.setTargetAmount(BigDecimal.ZERO);
        testGoal.setCurrentAmount(BigDecimal.ZERO);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(testGoal));

        SavingsGoalResponse result =
                savingsGoalService.getSavingsGoalById(1L, "test@example.com");

        assertThat(result.progressPercentage()).isZero();
    }
}
