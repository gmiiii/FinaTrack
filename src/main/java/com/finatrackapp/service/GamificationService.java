package com.finatrackapp.service;

import com.finatrackapp.dto.response.BadgeResponse;
import com.finatrackapp.dto.response.GamificationResponse;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.BadgeType;
import com.finatrackapp.model.Budget;
import com.finatrackapp.model.TransactionType;
import com.finatrackapp.model.User;
import com.finatrackapp.model.UserBadge;
import com.finatrackapp.repository.BudgetRepository;
import com.finatrackapp.repository.TransactionRepository;
import com.finatrackapp.repository.UserBadgeRepository;
import com.finatrackapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class GamificationService {

    private static final int WEEK_STREAK_THRESHOLD = 7;
    private static final int MONTH_STREAK_THRESHOLD = 30;
    private static final long CENTURY_TRANSACTION_THRESHOLD = 100;
    private static final int BUDGET_MASTER_MONTHS_REQUIRED = 3;

    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public GamificationService(UserRepository userRepository,
                               UserBadgeRepository userBadgeRepository,
                               TransactionRepository transactionRepository,
                               BudgetRepository budgetRepository) {
        this.userRepository = userRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    @Transactional
    public void updateStreakAndBadges(User user) {
        updateStreak(user);
        checkAndAwardBadges(user);
    }

    @Transactional
    public GamificationResponse getGamificationStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        checkBudgetMasterBadge(user);

        List<BadgeResponse> badges = userBadgeRepository.findByUserId(user.getId())
                .stream()
                .map(this::toBadgeResponse)
                .toList();

        return new GamificationResponse(
                user.getCurrentStreak(), user.getLongestStreak(), badges);
    }

    void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = user.getLastTransactionDate();

        if (lastDate == null) {
            user.setCurrentStreak(1);
        } else if (lastDate.equals(today)) {
            return;
        } else if (lastDate.equals(today.minusDays(1))) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            user.setCurrentStreak(1);
        }

        if (user.getCurrentStreak() > user.getLongestStreak()) {
            user.setLongestStreak(user.getCurrentStreak());
        }

        user.setLastTransactionDate(today);
        userRepository.save(user);
    }

    private void checkAndAwardBadges(User user) {
        long transactionCount = transactionRepository.countByUserId(user.getId());

        if (transactionCount == 1) {
            awardBadge(user, BadgeType.FIRST_TRANSACTION);
        }

        if (user.getCurrentStreak() >= WEEK_STREAK_THRESHOLD) {
            awardBadge(user, BadgeType.WEEK_STREAK);
        }

        if (user.getCurrentStreak() >= MONTH_STREAK_THRESHOLD) {
            awardBadge(user, BadgeType.MONTH_STREAK);
        }

        if (transactionCount >= CENTURY_TRANSACTION_THRESHOLD) {
            awardBadge(user, BadgeType.CENTURY_TRANSACTIONS);
        }
    }

    private void checkBudgetMasterBadge(User user) {
        if (userBadgeRepository.existsByUserIdAndBadgeType(
                user.getId(), BadgeType.BUDGET_MASTER)) {
            return;
        }

        LocalDate now = LocalDate.now();
        for (int i = 0; i < BUDGET_MASTER_MONTHS_REQUIRED; i++) {
            LocalDate checkDate = now.minusMonths(i);
            if (!isUnderBudgetForMonth(user.getId(),
                    checkDate.getMonthValue(), checkDate.getYear())) {
                return;
            }
        }

        awardBadge(user, BadgeType.BUDGET_MASTER);
    }

    private boolean isUnderBudgetForMonth(Long userId, int month, int year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
                userId, month, year);
        if (budgets.isEmpty()) {
            return false;
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        for (Budget budget : budgets) {
            BigDecimal spent = transactionRepository
                    .sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                            userId, budget.getCategory().getId(),
                            TransactionType.EXPENSE, startDate, endDate);
            if (spent.compareTo(budget.getMonthlyLimit()) > 0) {
                return false;
            }
        }

        return true;
    }

    void awardBadge(User user, BadgeType badgeType) {
        if (!userBadgeRepository.existsByUserIdAndBadgeType(
                user.getId(), badgeType)) {
            UserBadge badge = new UserBadge();
            badge.setUser(user);
            badge.setBadgeType(badgeType);
            userBadgeRepository.save(badge);
        }
    }

    private BadgeResponse toBadgeResponse(UserBadge userBadge) {
        return new BadgeResponse(
                userBadge.getBadgeType().name(),
                userBadge.getBadgeType().getDisplayName(),
                userBadge.getBadgeType().getDescription(),
                userBadge.getAwardedAt()
        );
    }
}
