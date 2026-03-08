package com.finatrackapp.model;

public enum BadgeType {
    FIRST_TRANSACTION("Pencatat Pertama", "Mencatat transaksi pertamamu"),
    WEEK_STREAK("Pejuang Mingguan", "Mencatat keuangan 7 hari berturut-turut"),
    MONTH_STREAK("Master Bulanan", "Mencatat keuangan 30 hari berturut-turut"),
    BUDGET_MASTER("Master Hemat", "Berhasil di bawah budget selama 3 bulan"),
    SAVINGS_STARTER("Penabung Pemula", "Membuat target tabungan pertama"),
    SAVINGS_ACHIEVER("Pencapai Target", "Menyelesaikan target tabungan"),
    CENTURY_TRANSACTIONS("Klub Seratus", "Mencatat 100 transaksi");

    private final String displayName;
    private final String description;

    BadgeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
