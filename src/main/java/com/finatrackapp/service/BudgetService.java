package com.finatrackapp.service;

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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {

    private static final int PERCENTAGE_SCALE = 2;
    private static final double WARNING_THRESHOLD = 80.0;
    private static final double FULL_PERCENTAGE = 100.0;
    private static final String STATUS_SAFE = "SAFE";
    private static final String STATUS_WARNING = "WARNING";
    private static final String STATUS_EXCEEDED = "EXCEEDED";

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository,
                         UserService userService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    public BudgetResponse createBudget(BudgetRequest request, String email) {
        User user = userService.getUserByEmail(email);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan"));

        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                user.getId(), request.getCategoryId(),
                request.getMonth(), request.getYear()
        ).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Budget untuk kategori ini di bulan tersebut sudah ada");
        });

        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setUser(user);
        budget.setMonthlyLimit(request.getMonthlyLimit());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        budgetRepository.save(budget);

        return toResponse(budget);
    }

    public List<BudgetResponse> getBudgets(String email, int month, int year) {
        User user = userService.getUserByEmail(email);
        return budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BudgetResponse updateBudget(Long id, BudgetRequest request, String email) {
        User user = userService.getUserByEmail(email);
        Budget budget = findUserBudget(id, user.getId());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan"));

        budget.setCategory(category);
        budget.setMonthlyLimit(request.getMonthlyLimit());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        budgetRepository.save(budget);

        return toResponse(budget);
    }

    public void deleteBudget(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Budget budget = findUserBudget(id, user.getId());
        budgetRepository.delete(budget);
    }

    public List<BudgetStatusResponse> getBudgetStatus(String email, int month, int year) {
        User user = userService.getUserByEmail(email);
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
                user.getId(), month, year);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return budgets.stream()
                .map(budget -> calculateBudgetStatus(
                        budget, user.getId(), startDate, endDate))
                .toList();
    }

    private BudgetStatusResponse calculateBudgetStatus(Budget budget, Long userId,
                                                        LocalDate startDate,
                                                        LocalDate endDate) {
        BigDecimal amountSpent = transactionRepository
                .sumAmountByUserIdAndCategoryIdAndTypeAndDateBetween(
                        userId, budget.getCategory().getId(),
                        TransactionType.EXPENSE, startDate, endDate);

        BigDecimal remaining = budget.getMonthlyLimit().subtract(amountSpent);

        double percentageUsed = amountSpent
                .multiply(BigDecimal.valueOf(FULL_PERCENTAGE))
                .divide(budget.getMonthlyLimit(), PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();

        String status = determineStatus(percentageUsed);

        return new BudgetStatusResponse(
                budget.getId(),
                budget.getCategory().getName(),
                budget.getMonthlyLimit(),
                amountSpent,
                remaining,
                percentageUsed,
                status
        );
    }

    private String determineStatus(double percentageUsed) {
        if (percentageUsed >= FULL_PERCENTAGE) {
            return STATUS_EXCEEDED;
        }
        if (percentageUsed >= WARNING_THRESHOLD) {
            return STATUS_WARNING;
        }
        return STATUS_SAFE;
    }

    private Budget findUserBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Budget tidak ditemukan"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Budget tidak ditemukan");
        }

        return budget;
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getName(),
                budget.getCategory().getId(),
                budget.getMonthlyLimit(),
                budget.getMonth(),
                budget.getYear()
        );
    }
}
