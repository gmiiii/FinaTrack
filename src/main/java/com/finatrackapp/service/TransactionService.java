package com.finatrackapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finatrackapp.dto.request.TransactionRequest;
import com.finatrackapp.dto.response.CategorySummaryResponse;
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

@Service
@SuppressWarnings("null")
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final GamificationService gamificationService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              UserService userService,
                              GamificationService gamificationService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
        this.gamificationService = gamificationService;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, String email) {
        User user = userService.getUserByEmail(email);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan"));

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transaction.setUser(user);
        transactionRepository.save(transaction);

        gamificationService.updateStreakAndBadges(user);

        return toResponse(transaction);
    }

    @SuppressWarnings("java:S107")
    public PagedResponse<TransactionResponse> getTransactions(
            String email, int page, int size,
            String sortBy, String sortDir,
            TransactionType type,
            LocalDate startDate, LocalDate endDate) {

        User user = userService.getUserByEmail(email);
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Transaction> transactionPage = fetchTransactions(
                user.getId(), type, startDate, endDate, pageable);

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isLast()
        );
    }

    public TransactionResponse getTransactionById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Transaction transaction = findUserTransaction(id, user.getId());
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request,
                                                  String email) {
        User user = userService.getUserByEmail(email);
        Transaction transaction = findUserTransaction(id, user.getId());
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan"));

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setDate(request.getDate());
        transaction.setCategory(category);
        transactionRepository.save(transaction);

        return toResponse(transaction);
    }

    public void deleteTransaction(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Transaction transaction = findUserTransaction(id, user.getId());
        transactionRepository.delete(transaction);
    }

    public DashboardResponse getDashboard(String email) {
        User user = userService.getUserByEmail(email);
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndType(
                user.getId(), TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndType(
                user.getId(), TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<CategorySummaryResponse> expenseByCategory = buildCategorySummary(
                user.getId(), startOfMonth, endOfMonth);

        return new DashboardResponse(totalIncome, totalExpense, balance, expenseByCategory);
    }

    public MonthlySummaryResponse getMonthlySummary(String email, int month, int year) {
        User user = userService.getUserByEmail(email);
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal totalIncome = transactionRepository
                .sumAmountByUserIdAndTypeAndDateBetween(
                        user.getId(), TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository
                .sumAmountByUserIdAndTypeAndDateBetween(
                        user.getId(), TransactionType.EXPENSE, startDate, endDate);

        List<CategorySummaryResponse> expenseByCategory = buildCategorySummary(
                user.getId(), startDate, endDate);

        return new MonthlySummaryResponse(
                month, year, totalIncome, totalExpense,
                totalIncome.subtract(totalExpense), expenseByCategory
        );
    }

    private Page<Transaction> fetchTransactions(Long userId, TransactionType type,
                                                 LocalDate startDate, LocalDate endDate,
                                                 Pageable pageable) {
        if (startDate != null && endDate != null) {
            return transactionRepository.findByUserIdAndDateBetween(
                    userId, startDate, endDate, pageable);
        }
        if (type != null) {
            return transactionRepository.findByUserIdAndType(userId, type, pageable);
        }
        return transactionRepository.findByUserId(userId, pageable);
    }

    private List<CategorySummaryResponse> buildCategorySummary(
            Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> categoryData = transactionRepository
                .sumAmountGroupByCategoryAndDateBetween(
                        userId, TransactionType.EXPENSE, startDate, endDate);

        return categoryData.stream()
                .map(row -> new CategorySummaryResponse(
                        (String) row[0],
                        (BigDecimal) row[1],
                        (Long) row[2]))
                .toList();
    }

    private Transaction findUserTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaksi tidak ditemukan"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaksi tidak ditemukan");
        }

        return transaction;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getDate(),
                transaction.getCategory().getName(),
                transaction.getCategory().getId(),
                transaction.getCreatedAt()
        );
    }
}
