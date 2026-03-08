package com.finatrackapp.service;

import com.finatrackapp.model.Transaction;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final String CSV_HEADER = "Tanggal,Tipe,Kategori,Nominal,Deskripsi";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public ExportService(TransactionRepository transactionRepository,
                         UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    public String exportTransactionsCsv(String email) {
        User user = userService.getUserByEmail(email);
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            writer.println(CSV_HEADER);

            for (Transaction tx : transactions) {
                writer.printf("%s,%s,%s,%s,%s%n",
                        tx.getDate().format(DATE_FORMATTER),
                        tx.getType(),
                        escapeCsv(tx.getCategory().getName()),
                        tx.getAmount().toPlainString(),
                        escapeCsv(tx.getDescription() != null ? tx.getDescription() : ""));
            }

            writer.flush();
        }
        return stringWriter.toString();
    }

    String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
