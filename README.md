# FinaTrack — Aplikasi Pencatatan Keuangan Pribadi

Aplikasi **full-stack** pencatatan dan pengelolaan keuangan pribadi, dibangun menggunakan **Spring Boot 3.4.0** (Java 17) untuk backend REST API dan **Vanilla JavaScript SPA** untuk frontend. Aplikasi ini menyediakan fitur lengkap mulai dari pencatatan transaksi, pengelolaan anggaran, target tabungan, hingga sistem gamifikasi untuk memotivasi pengguna.

---

## Narasi Proyek

### Latar Belakang

**FinaTrack** adalah aplikasi keuangan pribadi yang dirancang untuk membantu pengguna mencatat, memantau, dan mengelola keuangan mereka secara terstruktur. Aplikasi ini lahir dari kebutuhan akan solusi pengelolaan uang yang sederhana namun komprehensif — menggabungkan fitur pencatatan transaksi harian, perencanaan anggaran bulanan, penetapan target tabungan, dan elemen gamifikasi yang membuat proses pencatatan keuangan menjadi lebih menyenangkan.

### Arsitektur Aplikasi

Proyek ini menerapkan arsitektur **layered/tiered** yang umum digunakan pada aplikasi Spring Boot enterprise:

```
┌──────────────────────────────────────────────────────────────┐
│                    FRONTEND (SPA)                             │
│  index.html + style.css + api.js + app.js                    │
│  Served as static resources dari Spring Boot                 │
└──────────────────┬───────────────────────────────────────────┘
                   │ HTTP REST (JSON + JWT Bearer Token)
┌──────────────────▼───────────────────────────────────────────┐
│                 SPRING BOOT BACKEND                          │
│                                                              │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────────┐    │
│  │ Controller   │──▶│  Service     │──▶│  Repository     │    │
│  │ (REST API)   │   │ (Bisnis)    │   │ (Data Access)   │    │
│  └─────────────┘   └──────┬──────┘   └────────┬────────┘    │
│                           │                    │             │
│  ┌─────────────┐   ┌──────▼──────┐   ┌────────▼────────┐    │
│  │ Security    │   │  Exception  │   │  H2 Database    │    │
│  │ (JWT+BCrypt)│   │  Handler    │   │  (In-Memory)    │    │
│  └─────────────┘   └─────────────┘   └─────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

**Alur kerja:**
1. **Frontend** (HTML/CSS/JS) di-serve langsung oleh Spring Boot sebagai static resources.
2. Setiap request API dari frontend dikirim ke **Controller** dengan JWT token di header.
3. **Security Filter** memvalidasi token sebelum request diteruskan.
4. **Controller** meneruskan ke **Service** yang berisi logika bisnis.
5. **Service** mengakses data melalui **Repository** (Spring Data JPA).
6. Jika terjadi error, **GlobalExceptionHandler** menangkapnya dan mengembalikan response JSON yang konsisten.

### Teknologi & Alasan Pemilihan

| Komponen | Teknologi | Alasan |
|----------|-----------|--------|
| Framework | Spring Boot 3.4.0 | Framework Java paling mature untuk REST API, ekosistem luas |
| Bahasa | Java 17 | LTS terbaru dengan fitur records, sealed classes |
| Build Tool | Maven | Standar industri, dependency management yang solid |
| Autentikasi | Spring Security + JWT (jjwt 0.12.6) | Stateless auth cocok untuk SPA, standard industri |
| Database | H2 In-Memory | Zero-config untuk development, data reset otomatis |
| ORM | Spring Data JPA / Hibernate | Abstraksi database yang powerful, JPQL untuk query kompleks |
| Validasi | Jakarta Bean Validation | Declarative validation dengan anotasi |
| Password | BCrypt | Algoritma hashing yang aman dan proven |
| Frontend | Vanilla HTML/CSS/JS | Tanpa framework tambahan, ringan, mudah dipahami |
| Unit Test | JUnit 5 + Mockito + AssertJ | Stack testing standar Spring Boot |

### Fitur Utama

1. **Autentikasi & Keamanan** — Registrasi, login, JWT token, password hashing BCrypt. Setiap user hanya dapat mengakses data miliknya sendiri (data isolation).

2. **Kategori Transaksi** — Terdapat kategori bawaan sistem (**Makanan**, **Transportasi**, **Hiburan**, dll) yang bisa diakses semua user, serta kategori kustom yang dibuat oleh masing-masing user.

3. **Pencatatan Transaksi** — CRUD transaksi dengan tipe INCOME/EXPENSE, didukung pagination, sorting, dan filtering berdasarkan tipe serta rentang tanggal.

4. **Dashboard Keuangan** — Ringkasan total pemasukan, pengeluaran, saldo, dan breakdown pengeluaran per kategori.

5. **Anggaran Bulanan** — Pengguna dapat menetapkan batas pengeluaran per kategori per bulan. Sistem otomatis menghitung status: **AMAN** (<80%), **PERINGATAN** (80-99%), **MELEBIHI** (≥100%).

6. **Target Tabungan** — Fitur penetapan target tabungan dengan tracking kontribusi. Sistem menghitung persentase pencapaian secara real-time.

7. **Gamifikasi** — Sistem streak harian dan 7 jenis badge untuk memotivasi konsistensi pencatatan keuangan (Transaksi Pertama, Seminggu Konsisten, Sebulan Konsisten, Ahli Anggaran, Penabung Pemula, Target Tercapai, Transaksi Seabad).

8. **Ekspor CSV** — Download seluruh riwayat transaksi dalam format CSV.

9. **Frontend SPA** — Antarmuka mobile-first dengan tema ungu, navigasi tab bawah, animasi smooth, dan semua fitur backend dapat diakses langsung melalui browser.

### Kualitas Kode & Standar

Proyek ini telah melalui audit kepatuhan **SonarQube** dan menerapkan praktik berikut:

- **Tidak ada hardcoded secrets** — JWT secret menggunakan environment variable (`${JWT_SECRET}`) dengan fallback untuk development.
- **Konfigurasi berbasis profil** — H2 console dan SQL logging hanya aktif di profil `dev`, nonaktif secara default.
- **CSRF dinonaktifkan secara aman** — REST API stateless berbasis JWT tidak memerlukan CSRF protection, didokumentasikan dengan jelas.
- **H2 console dibatasi** — Hanya accessible ketika profil dev aktif.
- **Resource management** — Semua I/O resources (Writer, Stream) menggunakan try-with-resources.
- **Tidak ada magic numbers** — Semua angka konstanta diekstrak ke named constants.
- **Tidak ada string duplikat** — Pesan error yang berulang diekstrak ke konstanta.
- **Exception handling lengkap** — Semua exception memiliki pesan deskriptif, ditangani oleh GlobalExceptionHandler termasuk fallback untuk exception generik.
- **Logging yang tepat** — Exception di-log bukan di-swallow, level log disesuaikan konteksnya.
- **Tidak ada dead code** — Method repository yang tidak terpakai sudah dihapus.

### Pengujian

Proyek memiliki **72 unit test** yang mencakup seluruh business logic di layer service:

| Modul | Test | Cakupan |
|-------|------|---------|
| Auth | 5 | Registrasi, duplikat email, encoding password, login, kredensial salah |
| User | 4 | Ambil profil, user tidak ditemukan, update profil |
| Kategori | 12 | CRUD, proteksi kategori sistem, validasi kepemilikan |
| Transaksi | 12 | CRUD, pagination, filter, dashboard, ringkasan bulanan |
| Anggaran | 10 | CRUD, duplikat, status AMAN/PERINGATAN/MELEBIHI |
| Tabungan | 11 | CRUD, kontribusi, melebihi target, badge otomatis |
| Gamifikasi | 10 | Streak, badge, budget master |
| Ekspor | 7 | CSV kosong, format, karakter khusus, escape |

Semua test menggunakan **Mockito** untuk isolasi dari database dan **AssertJ** untuk assertion yang ekspresif.

---

## Daftar Isi

- [Prasyarat](#prasyarat)
- [Cara Menjalankan](#cara-menjalankan)
- [Cara Menjalankan Test](#cara-menjalankan-test)
- [Struktur Proyek](#struktur-proyek)
- [Konfigurasi](#konfigurasi)
- [Autentikasi](#autentikasi)
- [Daftar API Endpoint](#daftar-api-endpoint)
- [Penjelasan Fitur](#penjelasan-fitur)
- [Database](#database)
- [Catatan untuk Developer Selanjutnya](#catatan-untuk-developer-selanjutnya)

---

## Prasyarat

- **Java 17** atau lebih baru
- **Apache Maven 3.8+**

Pastikan kedua tools tersebut sudah terinstall dan tersedia di PATH:

```bash
java -version
mvn -version
```

---

## Cara Menjalankan

### Mode Development (Rekomendasi)

```bash
# Clone/buka proyek, lalu jalankan:
mvn spring-boot:run
```

Secara default, profil `dev` aktif sehingga:
- H2 Console tersedia di `http://localhost:8080/h2-console`
- SQL logging aktif di console

Aplikasi berjalan di **`http://localhost:8080`** — buka URL ini di browser untuk mengakses frontend.

### Mode Production

Untuk menjalankan tanpa fitur development:

```bash
# Menggunakan environment variable
set SPRING_PROFILES_ACTIVE=prod
set JWT_SECRET=GantiDenganSecretKeyAndaYangAmanDanPanjangMinimal256Bit!!
mvn spring-boot:run
```

Atau build menjadi JAR dan jalankan:

```bash
mvn clean package -DskipTests
java -jar target/FinaTrack-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --jwt.secret=SECRET_ANDA
```

### Konfigurasi Profil

| Profil | H2 Console | SQL Logging | JWT Secret | Penggunaan |
|--------|-----------|------------|------------|------------|
| `dev` (default) | ✅ Aktif | ✅ Aktif | Fallback bawaan | Development lokal |
| `prod` | ❌ Nonaktif | ❌ Nonaktif | **Wajib** dari env var | Production |

### Verifikasi

Setelah aplikasi berjalan:

1. **Frontend**: Buka `http://localhost:8080` di browser — akan tampil halaman login FinaTrack
2. **Health check API**: `GET http://localhost:8080/api/health` — harus mengembalikan status OK
3. **H2 Console** (dev only): `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:finatrackdb`
   - Username: `sa`
   - Password: *(kosong)*

---

## Cara Menjalankan Test

```bash
# Jalankan semua unit test
mvn test
```

Total: **72 unit test** di 9 test class. Semua harus berstatus PASS.

Output yang diharapkan:
```
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Struktur Proyek

```
src/main/
├── java/com/finatrackapp/
│   ├── FinaTrackApplication.java        ← Entry point aplikasi
│   │
│   ├── config/                          ← Konfigurasi keamanan & JWT
│   │   ├── SecurityConfig.java          ← Spring Security (CSRF off, stateless, profil H2)
│   │   ├── JwtAuthenticationFilter.java ← Filter validasi JWT di setiap request
│   │   └── CustomUserDetailsService.java← Loader user dari DB untuk Spring Security
│   │
│   ├── util/
│   │   └── JwtUtil.java                 ← Pembuatan & validasi token JWT
│   │
│   ├── controller/                      ← REST Controller (HTTP handler)
│   │   ├── HealthController.java        ← GET /api/health
│   │   ├── AuthController.java          ← Registrasi & login
│   │   ├── UserController.java          ← Profil pengguna
│   │   ├── CategoryController.java      ← CRUD kategori
│   │   ├── TransactionController.java   ← CRUD transaksi, dashboard, ringkasan
│   │   ├── BudgetController.java        ← CRUD anggaran, status anggaran
│   │   ├── SavingsGoalController.java   ← CRUD target tabungan, kontribusi
│   │   ├── GamificationController.java  ← Status gamifikasi (streak & badge)
│   │   └── ExportController.java        ← Ekspor transaksi ke CSV
│   │
│   ├── service/                         ← Business logic
│   │   ├── AuthService.java             ← Registrasi & login
│   │   ├── UserService.java             ← Profil pengguna
│   │   ├── CategoryService.java         ← CRUD kategori (sistem & kustom)
│   │   ├── TransactionService.java      ← Transaksi, dashboard, ringkasan
│   │   ├── BudgetService.java           ← Anggaran & pengecekan status
│   │   ├── SavingsGoalService.java      ← Target tabungan & kontribusi
│   │   ├── GamificationService.java     ← Streak harian & pemberian badge
│   │   └── ExportService.java           ← Pembuatan file CSV
│   │
│   ├── repository/                      ← Data access layer (Spring Data JPA)
│   │   ├── UserRepository.java
│   │   ├── CategoryRepository.java
│   │   ├── TransactionRepository.java
│   │   ├── BudgetRepository.java
│   │   ├── SavingsGoalRepository.java
│   │   └── UserBadgeRepository.java
│   │
│   ├── model/                           ← Entity JPA & Enum
│   │   ├── User.java                    ← Tabel "users"
│   │   ├── Category.java                ← Tabel "categories"
│   │   ├── Transaction.java             ← Tabel "transactions"
│   │   ├── Budget.java                  ← Tabel "budgets"
│   │   ├── SavingsGoal.java             ← Tabel "savings_goals"
│   │   ├── UserBadge.java               ← Tabel "user_badges"
│   │   ├── TransactionType.java         ← Enum: INCOME, EXPENSE
│   │   └── BadgeType.java               ← Enum: 7 jenis badge
│   │
│   ├── dto/
│   │   ├── request/                     ← DTO request body (dengan validasi)
│   │   │   ├── RegisterRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── UpdateProfileRequest.java
│   │   │   ├── CategoryRequest.java
│   │   │   ├── TransactionRequest.java
│   │   │   ├── BudgetRequest.java
│   │   │   ├── SavingsGoalRequest.java
│   │   │   └── SavingsGoalContributionRequest.java
│   │   │
│   │   └── response/                    ← DTO response body (Java records)
│   │       ├── ApiResponse.java         ← Wrapper: {success, message, data}
│   │       ├── AuthResponse.java
│   │       ├── UserResponse.java
│   │       ├── CategoryResponse.java
│   │       ├── TransactionResponse.java
│   │       ├── BudgetResponse.java
│   │       ├── BudgetStatusResponse.java
│   │       ├── SavingsGoalResponse.java
│   │       ├── DashboardResponse.java
│   │       ├── MonthlySummaryResponse.java
│   │       ├── CategorySummaryResponse.java
│   │       ├── GamificationResponse.java
│   │       ├── BadgeResponse.java
│   │       └── PagedResponse.java
│   │
│   └── exception/                       ← Exception handling
│       ├── ResourceNotFoundException.java    ← 404
│       ├── DuplicateResourceException.java   ← 409
│       ├── InvalidOperationException.java    ← 400
│       ├── BadRequestException.java          ← 400
│       └── GlobalExceptionHandler.java       ← @RestControllerAdvice
│
├── resources/
│   ├── application.properties           ← Konfigurasi utama (default)
│   ├── application-dev.properties       ← Override khusus development
│   └── static/                          ← Frontend SPA
│       ├── index.html                   ← Halaman utama (single-page)
│       ├── css/style.css                ← Styling (tema ungu, mobile-first)
│       └── js/
│           ├── api.js                   ← API service layer (fetch + JWT)
│           └── app.js                   ← Logika aplikasi & UI rendering
│
└── test/java/com/finatrackapp/
    ├── FinaTrackApplicationTests.java    ← Context loading test (1)
    └── service/
        ├── AuthServiceTest.java          ← 5 test
        ├── UserServiceTest.java          ← 4 test
        ├── CategoryServiceTest.java      ← 12 test
        ├── TransactionServiceTest.java   ← 12 test
        ├── BudgetServiceTest.java        ← 10 test
        ├── SavingsGoalServiceTest.java   ← 11 test
        ├── GamificationServiceTest.java  ← 10 test
        └── ExportServiceTest.java        ← 7 test
```

---

## Konfigurasi

### File Konfigurasi

**`application.properties`** (default — berlaku untuk semua profil):

| Property | Nilai | Keterangan |
|----------|-------|------------|
| `server.port` | `8080` | Port server |
| `spring.datasource.url` | `jdbc:h2:mem:finatrackdb` | Database H2 in-memory |
| `spring.datasource.username` | `sa` | Username DB |
| `spring.datasource.password` | *(kosong)* | Password DB |
| `spring.h2.console.enabled` | `false` | H2 console nonaktif secara default |
| `spring.jpa.show-sql` | `false` | SQL logging nonaktif secara default |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-create/update tabel |
| `jwt.secret` | `${JWT_SECRET:fallback}` | Secret key JWT dari env var, ada fallback dev |
| `jwt.expiration-ms` | `86400000` | Masa berlaku token (24 jam) |
| `spring.profiles.active` | `${SPRING_PROFILES_ACTIVE:dev}` | Profil aktif, default = dev |

**`application-dev.properties`** (override saat profil `dev` aktif):

| Property | Nilai | Keterangan |
|----------|-------|------------|
| `spring.h2.console.enabled` | `true` | H2 console aktif |
| `spring.jpa.show-sql` | `true` | SQL logging aktif |

---

## Autentikasi

Sistem menggunakan **JWT (JSON Web Token)** dengan alur:

1. User melakukan **registrasi** (`POST /api/auth/register`) — password di-hash dengan BCrypt.
2. User melakukan **login** (`POST /api/auth/login`) — server mengembalikan token JWT.
3. Untuk semua endpoint selain `/api/auth/**`, sertakan header:
   ```
   Authorization: Bearer <token_jwt>
   ```
4. Token berlaku **24 jam**. Setelah expired, user harus login ulang.

### Endpoint Publik (Tanpa Token)

- `GET /` — Frontend SPA
- `GET /api/health` — Health check
- `POST /api/auth/register` — Registrasi
- `POST /api/auth/login` — Login
- `GET /h2-console/**` — Database console (hanya profil dev)

### Endpoint Terproteksi (Butuh Token)

Semua endpoint lainnya. Data otomatis difilter berdasarkan user yang login.

---

## Daftar API Endpoint

### Auth

| Method | Endpoint | Deskripsi | Body |
|--------|----------|-----------|------|
| POST | `/api/auth/register` | Registrasi user baru | `fullName`, `email`, `password` |
| POST | `/api/auth/login` | Login & dapatkan token | `email`, `password` |

### User

| Method | Endpoint | Deskripsi | Body |
|--------|----------|-----------|------|
| GET | `/api/users/profile` | Lihat profil sendiri | — |
| PUT | `/api/users/profile` | Update profil | `fullName` |

### Kategori

| Method | Endpoint | Deskripsi | Body |
|--------|----------|-----------|------|
| GET | `/api/categories` | Daftar semua kategori (sistem + kustom) | — |
| POST | `/api/categories` | Buat kategori baru | `name`, `type` (INCOME/EXPENSE) |
| GET | `/api/categories/{id}` | Detail kategori | — |
| PUT | `/api/categories/{id}` | Update kategori (hanya milik sendiri) | `name`, `type` |
| DELETE | `/api/categories/{id}` | Hapus kategori (hanya milik sendiri) | — |

### Transaksi

| Method | Endpoint | Deskripsi | Parameter/Body |
|--------|----------|-----------|----------------|
| GET | `/api/transactions` | Daftar transaksi (paginasi) | Query: `page`, `size`, `sortBy`, `sortDir`, `type`, `startDate`, `endDate` |
| POST | `/api/transactions` | Catat transaksi baru | `amount`, `type`, `description`, `date`, `categoryId` |
| GET | `/api/transactions/{id}` | Detail transaksi | — |
| PUT | `/api/transactions/{id}` | Update transaksi | `amount`, `type`, `description`, `date`, `categoryId` |
| DELETE | `/api/transactions/{id}` | Hapus transaksi | — |
| GET | `/api/transactions/dashboard` | Dashboard keuangan | — |
| GET | `/api/transactions/summary` | Ringkasan bulanan | Query: `month`, `year` |

### Anggaran (Budget)

| Method | Endpoint | Deskripsi | Parameter/Body |
|--------|----------|-----------|----------------|
| POST | `/api/budgets` | Buat anggaran bulanan per kategori | `categoryId`, `monthlyLimit`, `month`, `year` |
| GET | `/api/budgets` | Daftar anggaran | — |
| PUT | `/api/budgets/{id}` | Update anggaran | `categoryId`, `monthlyLimit`, `month`, `year` |
| DELETE | `/api/budgets/{id}` | Hapus anggaran | — |
| GET | `/api/budgets/status` | Status semua anggaran bulan tertentu | Query: `month`, `year` |

**Status Anggaran:**
- **AMAN** — pengeluaran < 80% dari limit
- **PERINGATAN** — pengeluaran 80%–99% dari limit
- **MELEBIHI** — pengeluaran ≥ 100% dari limit

### Target Tabungan (Savings Goal)

| Method | Endpoint | Deskripsi | Body |
|--------|----------|-----------|------|
| POST | `/api/savings-goals` | Buat target tabungan | `name`, `targetAmount`, `targetDate` |
| GET | `/api/savings-goals` | Daftar target tabungan | — |
| GET | `/api/savings-goals/{id}` | Detail target | — |
| PUT | `/api/savings-goals/{id}` | Update target | `name`, `targetAmount`, `targetDate` |
| DELETE | `/api/savings-goals/{id}` | Hapus target | — |
| POST | `/api/savings-goals/{id}/contribute` | Tambah kontribusi ke target | `amount` |

### Gamifikasi

| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET | `/api/gamification/status` | Lihat streak, badge |

**Daftar Badge:**
| Badge | Syarat |
|-------|--------|
| Transaksi Pertama | Mencatat transaksi pertama kali |
| Seminggu Konsisten | Streak ≥ 7 hari berturut-turut |
| Sebulan Konsisten | Streak ≥ 30 hari berturut-turut |
| Ahli Anggaran | Tidak melebihi anggaran 3 bulan berturut-turut |
| Penabung Pemula | Membuat target tabungan pertama |
| Target Tercapai | Mencapai salah satu target tabungan |
| Transaksi Seabad | Mencatat total 100 transaksi |

### Ekspor

| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET | `/api/export/transactions` | Download transaksi format CSV |

---

## Penjelasan Fitur

### 1. Isolasi Data per User
Setiap user hanya bisa melihat dan mengelola data miliknya sendiri. Validasi kepemilikan dilakukan di layer service sebelum operasi update/delete.

### 2. Kategori Sistem vs Kustom
- **Kategori sistem**: field `user` = null, dapat diakses semua user, tidak bisa diedit/dihapus.
- **Kategori kustom**: milik user tertentu, hanya pemilik yang bisa edit/hapus.

### 3. Pagination & Filtering pada Transaksi
Endpoint `GET /api/transactions` mendukung:
- `page` (default: 0) dan `size` (default: 10) untuk pagination
- `sortBy` (default: date) dan `sortDir` (default: desc) untuk pengurutan
- `type` (INCOME/EXPENSE) untuk filter jenis transaksi
- `startDate` dan `endDate` (format: yyyy-MM-dd) untuk filter rentang tanggal

### 4. Dashboard
Mengembalikan ringkasan keuangan keseluruhan: total pemasukan, total pengeluaran, saldo, dan breakdown pengeluaran per kategori.

### 5. Streak Harian
Sistem melacak berapa hari berturut-turut user mencatat transaksi. Streak bertambah jika user mencatat transaksi di hari berikutnya, reset ke 1 jika ada hari yang terlewat.

### 6. Ekspor CSV
Format output:
```
Tanggal,Tipe,Kategori,Nominal,Deskripsi
2026-03-08,EXPENSE,Makanan,50000,Makan siang
```
Karakter khusus (koma, kutip, newline) otomatis di-escape sesuai standar CSV.

### 7. Frontend SPA
Antarmuka web single-page dengan:
- Tema ungu (`#4A3AFF`) yang konsisten
- Desain mobile-first (max-width 480px)
- Bottom navigation bar untuk navigasi antar halaman
- Modal bottom-sheet untuk form input
- Semua fitur backend dapat diakses tanpa tools tambahan

---

## Database

### Engine
- **Development**: H2 In-Memory (data hilang saat server restart)
- Skema tabel dibuat otomatis oleh Hibernate (`ddl-auto=update`)

### Akses H2 Console (Dev Only)
1. Pastikan profil `dev` aktif (default)
2. Buka `http://localhost:8080/h2-console`
3. JDBC URL: `jdbc:h2:mem:finatrackdb`
4. Username: `sa`, Password: *(kosong)*

### Entity Relationship

```
users
├── categories (one-to-many, nullable — null = kategori sistem)
├── transactions (one-to-many)
├── budgets (one-to-many)
├── savings_goals (one-to-many)
└── user_badges (one-to-many)

categories
├── transactions (one-to-many)
└── budgets (one-to-many)
```

### Constraint Penting
- `budgets`: Unique pada (`user_id`, `category_id`, `budget_month`, `budget_year`)
- `user_badges`: Unique pada (`user_id`, `badge_type`)

---

## Format Response Standar

Semua endpoint mengembalikan format yang sama:

```json
{
  "success": true,
  "message": "Pesan sukses/error",
  "data": { ... }
}
```

Untuk pagination:

```json
{
  "success": true,
  "message": "...",
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 50,
    "totalPages": 5,
    "last": false
  }
}
```

---

## Catatan untuk Developer Selanjutnya

### Menjalankan di Lingkungan Baru

```bash
# 1. Pastikan Java 17 & Maven terinstall
java -version   # harus 17+
mvn -version    # harus 3.8+

# 2. Jalankan test untuk verifikasi
mvn test        # harus 72/72 PASS

# 3. Jalankan aplikasi
mvn spring-boot:run

# 4. Buka browser → http://localhost:8080
```

### Mengganti Database ke Production

1. Tambahkan dependency database di `pom.xml` (misal: `postgresql`).
2. Buat `application-prod.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/finatrack
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=validate
   ```
3. Gunakan migration tool (**Flyway** / **Liquibase**) untuk mengelola skema.

### Menambah Fitur Baru

1. Buat entity di `model/` → DTO di `dto/` → Repository di `repository/`
2. Implementasi business logic di `service/`
3. Expose melalui `controller/`
4. Buat unit test di `src/test/java/.../service/`
5. Endpoint otomatis terproteksi JWT (kecuali ditambahkan ke permitAll)

### Hal yang Perlu Diperhatikan

- **JWT Secret**: Di production, **wajib** set environment variable `JWT_SECRET`. Jangan gunakan fallback bawaan.
- **H2 Database**: Data hilang setiap restart. Wajib gunakan database persisten untuk production.
- **CORS**: Belum dikonfigurasi. Jika frontend diakses dari domain berbeda, tambahkan konfigurasi CORS di `SecurityConfig.java`.
- **Rate Limiting**: Belum ada. Pertimbangkan jika di-deploy ke publik.
- **Logging**: Default Spring Boot logging. Untuk production, konfigurasi level log dan output ke file.
- **Pesan Error**: Semua pesan validasi dan error menggunakan bahasa Indonesia.
