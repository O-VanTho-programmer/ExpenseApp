# Antigravity Expense Tracker: Technical Specification

## 1. System Overview
An automated personal expense and income tracker built natively for Android. The application intercepts transaction events locally using Android's notification subsystem, parses unstructured text into normalized financial records, performs transfer deduplication, and persists data inside an encrypted local database.

---

## 2. Ingestion Strategy: Push Notification Interception

### Rationale
* Direct SMS interception (`READ_SMS` / `RECEIVE_SMS`) is prohibited on Google Play unless registered as the default telephony application.
* Banking apps, fintech wallets, and the default system SMS app all post notifications to the Android status bar.
* Using `NotificationListenerService` enables real-time, background, on-device ingestion without incurring recurring API fees (e.g., Plaid/Salt Edge).

### Technical Ingestion Flow
1. **Target Package Filtering**: Listen exclusively to registered package names (e.g., banking apps, fintech e-wallets, or `com.google.android.apps.messaging` for SMS fallbacks).
2. **Payload Extraction**: Read notification extras:
   * `Notification.EXTRA_TITLE`
   * `Notification.EXTRA_TEXT`
   * `Notification.EXTRA_BIG_TEXT`
   * Timestamp & Package origin.
3. **Filter Noise**: Drop alerts that do not contain financial signals (e.g., promotional campaigns, OTP codes, login notices).

---

## 3. Parsing Engine & Transaction Classification

### Extracted Transaction Schema
```kotlin
enum class TransactionType { DEBIT, CREDIT, TRANSFER }

data class RawNotificationPayload(
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long
)

data class ParsedTransaction(
    val rawId: String,
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val counterparty: String?,
    val accountIdentifier: String?, // e.g., last 4 digits
    val timestamp: Long
)
```

### Classification Pipeline
1. **Amount Normalization**: Extract currency symbols (`$`, `€`, `VND`, `USD`) and numbers with separators (`1,250.00`, `50.000đ`).
2. **Direction Classification**:
   * **Debit (Expense)**: Keywords like `paid`, `spent`, `debited`, `charged`, `purchase`, `-`.
   * **Credit (Income)**: Keywords like `received`, `credited`, `deposit`, `refund`, `+`.
3. **Self-Transfer Deduplication**:
   * If a `DEBIT` of amount $A$ occurs at $T_1$ and a `CREDIT` of amount $A$ occurs at $T_2$ where $|T_1 - T_2| \le 180\text{ seconds}$, link them as a single `TRANSFER` record. This prevents artificial inflation of monthly income and expenses.

---

## 4. Database Schema (Room / SQLite)

```sql
-- Accounts (Source of funds)
CREATE TABLE accounts (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL, -- CHECKING, SAVINGS, CREDIT_CARD, WALLET, CASH
    currency TEXT NOT NULL DEFAULT 'USD',
    current_balance REAL NOT NULL DEFAULT 0.0,
    identifier_mask TEXT -- Last 4 digits or card tail
);

-- Categories
CREATE TABLE categories (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL, -- EXPENSE, INCOME
    icon_key TEXT,
    color_hex TEXT
);

-- Normalized Transactions
CREATE TABLE transactions (
    id TEXT PRIMARY KEY NOT NULL,
    account_id TEXT NOT NULL,
    destination_account_id TEXT, -- Populated if TRANSFER
    category_id TEXT,
    amount REAL NOT NULL,
    currency TEXT NOT NULL,
    type TEXT NOT NULL, -- DEBIT, CREDIT, TRANSFER
    counterparty TEXT,
    raw_text TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'CONFIRMED', -- PENDING, CONFIRMED, EXCLUDED
    FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY(destination_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Parsing Patterns
CREATE TABLE parsing_rules (
    id TEXT PRIMARY KEY NOT NULL,
    package_name TEXT NOT NULL,
    regex_pattern TEXT NOT NULL,
    amount_group_index INTEGER NOT NULL,
    type_group_index INTEGER NOT NULL,
    counterparty_group_index INTEGER,
    account_group_index INTEGER
);
```

---

## 5. Security & Privacy Safeguards
* **Encrypted Storage**: Room DB backed by **SQLCipher** with a key protected by the Android Keystore.
* **Local-Only Processing**: Zero network transmission of raw text notifications.
* **PII Redaction**: Discard or mask card numbers and balance snippets from crash reporting and diagnostics.