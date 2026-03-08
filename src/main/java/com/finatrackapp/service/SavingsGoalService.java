package com.finatrackapp.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SavingsGoalService {

    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserService userService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              UserBadgeRepository userBadgeRepository,
                              UserService userService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userService = userService;
    }

    public SavingsGoalResponse createSavingsGoal(SavingsGoalRequest request, String email) {
        User user = userService.getUserByEmail(email);

        SavingsGoal goal = new SavingsGoal();
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setTargetDate(request.getTargetDate());
        goal.setUser(user);
        savingsGoalRepository.save(goal);

        awardBadgeIfNew(user, BadgeType.SAVINGS_STARTER);

        return toResponse(goal);
    }

    public List<SavingsGoalResponse> getSavingsGoals(String email) {
        User user = userService.getUserByEmail(email);
        return savingsGoalRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SavingsGoalResponse getSavingsGoalById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        SavingsGoal goal = findUserGoal(id, user.getId());
        return toResponse(goal);
    }

    public SavingsGoalResponse updateSavingsGoal(Long id, SavingsGoalRequest request,
                                                  String email) {
        User user = userService.getUserByEmail(email);
        SavingsGoal goal = findUserGoal(id, user.getId());

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        savingsGoalRepository.save(goal);

        return toResponse(goal);
    }

    public void deleteSavingsGoal(Long id, String email) {
        User user = userService.getUserByEmail(email);
        SavingsGoal goal = findUserGoal(id, user.getId());
        savingsGoalRepository.delete(goal);
    }

    @Transactional
    public SavingsGoalResponse contribute(Long id,
                                           SavingsGoalContributionRequest request,
                                           String email) {
        User user = userService.getUserByEmail(email);
        SavingsGoal goal = findUserGoal(id, user.getId());

        BigDecimal newAmount = goal.getCurrentAmount().add(request.getAmount());
        if (newAmount.compareTo(goal.getTargetAmount()) > 0) {
            throw new InvalidOperationException(
                    "Kontribusi melebihi target tabungan");
        }

        goal.setCurrentAmount(newAmount);
        savingsGoalRepository.save(goal);

        if (newAmount.compareTo(goal.getTargetAmount()) == 0) {
            awardBadgeIfNew(user, BadgeType.SAVINGS_ACHIEVER);
        }

        return toResponse(goal);
    }

    private void awardBadgeIfNew(User user, BadgeType badgeType) {
        if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), badgeType)) {
            UserBadge badge = new UserBadge();
            badge.setUser(user);
            badge.setBadgeType(badgeType);
            userBadgeRepository.save(badge);
        }
    }

    private SavingsGoal findUserGoal(Long goalId, Long userId) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target tabungan tidak ditemukan"));

        if (!goal.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Target tabungan tidak ditemukan");
        }

        return goal;
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        double progressPercentage = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = goal.getCurrentAmount()
                    .multiply(HUNDRED)
                    .divide(goal.getTargetAmount(), PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                progressPercentage,
                goal.getTargetDate()
        );
    }
}
