# Antigravity Expense Tracker (Android MVP)

[![Android CI](https://github.com/O-VanTho-programmer/ExpenseApp/actions/workflows/android.yml/badge.svg)](https://github.com/O-VanTho-programmer/ExpenseApp/actions/workflows/android.yml)

An automated personal expense and income tracker built natively for Android. The application intercepts transaction events locally using Android's notification subsystem, parses unstructured text into normalized financial records, performs self-transfer deduplication, and persists data inside an encrypted local database.

---

## 🚀 Key Features

* **Zero-Permission Notification Ingestion**: Intercepts real-time banking and fintech status-bar alerts via `NotificationListenerService` without requiring restricted `READ_SMS` / `RECEIVE_SMS` permissions.
* **Intelligent Transaction Parsing Engine**: Multi-lingual, multi-currency rule engine supporting US, European, and Vietnamese banking formats (Chase, Wells Fargo, Vietcombank, MB Bank, MoMo, ZaloPay, etc.).
* **Noise & Security Filter**: Automatically identifies and discards non-financial alerts, OTP verification codes, and marketing promotional notifications.
* **Self-Transfer Deduplication**: Features a dynamic 3-minute (180-second) pairing window that detects equal debit/credit pairs between accounts and unifies them into a single `TRANSFER` record to prevent artificial inflation of monthly figures.
* **Local-Only Encrypted Storage**: Fully on-device persistence powered by Room and **SQLCipher** with encryption keys guarded by the Android Keystore. Zero external network telemetry.
* **Modern Jetpack Compose UI**: Clean Material 3 design featuring:
  * **Dashboard**: Net balance, income/expense breakdown, search, and reactive feed.
  * **Financial Accounts**: Account management with masking (e.g. `•••• 4242`).
  * **Category Mapping**: Categorization based on detected counterparties and merchant types.
  * **Simulation Workbench**: Live interactive testbed to simulate push alerts and inspect parser output in real time.

---

## 🏛 System Architecture

```
                    ┌───────────────────────────────┐
                    │ Android Notification Subsystem│
                    └───────────────┬───────────────┘
                                    │
                                    ▼
                 ┌─────────────────────────────────────┐
                 │  TransactionNotificationListener    │
                 │      (Filters Noise & OTPs)         │
                 └──────────────────┬──────────────────┘
                                    │
                                    ▼
                 ┌─────────────────────────────────────┐
                 │      TransactionParserEngine        │
                 │   - Amount & Currency Normalizer    │
                 │   - Merchant / Counterparty Mapper  │
                 │   - Account Mask Identifier         │
                 └──────────────────┬──────────────────┘
                                    │
                                    ▼
                 ┌─────────────────────────────────────┐
                 │        TransferDeduplicator         │
                 │  (180s Debit/Credit Merging Window) │
                 └──────────────────┬──────────────────┘
                                    │
                                    ▼
                 ┌─────────────────────────────────────┐
                 │       Encrypted Room Database       │
                 │      (SQLCipher + Keystore)         │
                 └──────────────────┬──────────────────┘
                                    │
                                    ▼
                 ┌─────────────────────────────────────┐
                 │         Jetpack Compose UI          │
                 │   Dashboard • Accounts • Simulator  │
                 └─────────────────────────────────────┘
```

---

## 🗄 Database Schema

* **`accounts`**: Source/destination of funds (`CHECKING`, `SAVINGS`, `CREDIT_CARD`, `WALLET`, `CASH`), currency, balance, identifier mask.
* **`categories`**: Financial classification (`EXPENSE`, `INCOME`), icons, color codes.
* **`transactions`**: Normalized records (`DEBIT`, `CREDIT`, `TRANSFER`), foreign keys to accounts and categories, raw source text, timestamp, status.
* **`parsing_rules`**: Pattern storage for dynamic package-specific regex overrides.

---

## 🧪 Testing & Simulation

The test suite covers:
1. **Parser Regex & Normalization Suite**:
   * English Debit (`Paid $25.50 to Starbucks`, `Debited USD 12.00 at Uber`, etc.)
   * English Credit & P2P (`Received $500.00 from John`, `Sarah sent you $25.00`)
   * Vietnamese Banking & Fintech (`TK 1234 -50,000 VND tai Circle K`, `Thanh toan 65.000d tai HighLands Coffee`, `TK 9876 +2.000.000 VND`)
   * Noise Filtering (OTP verification codes, promotional vouchers, login notices)
2. **Transfer Deduplication Suite**:
   * Debit and Credit pairing within 180 seconds into unified `TRANSFER`.
   * Outside-window threshold verification.
   * Unequal amounts non-pairing verification.

Run tests via Gradle:
```bash
./gradlew test
```

Build debug APK:
```bash
./gradlew assembleDebug
```

---

## 🛠 Tech Stack

* **Language**: Kotlin 1.9.23
* **UI**: Jetpack Compose (BOM 2024.04.01), Material 3
* **Architecture**: MVVM, Repository Pattern, StateFlow / Coroutines
* **Database**: Android Jetpack Room 2.6.1 + SQLCipher 4.5.4
* **Target SDK**: Android 34 (Upside Down Cake), Min SDK: Android 26 (Oreo)
* **Build System**: Gradle 8.7 (Kotlin DSL, Version Catalog)
* **CI/CD**: GitHub Actions
