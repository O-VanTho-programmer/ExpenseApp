# Antigravity Expense Tracker: Implementation Guide

## 1. Android Manifest Permissions & Configuration

Add the notification listener service declaration inside `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Optional foreground service permission if persistent processing is needed -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <!-- Notification Listener Service Registration -->
        <service
            android:name=".service.TransactionNotificationListener"
            android:label="Expense Tracker Ingestion Service"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

---

## 2. Notification Ingestion Service

Create `TransactionNotificationListener.kt`:

```kotlin
package com.antigravity.expensetracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.antigravity.expensetracker.data.parser.TransactionParserEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parserEngine = TransactionParserEngine()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        val fullContent = if (bigText.isNotBlank()) bigText else text
        if (fullContent.isBlank()) return

        serviceScope.launch {
            parserEngine.processNotification(
                packageName = packageName,
                title = title,
                content = fullContent,
                postTime = sbn.postTime
            )
        }
    }
}
```

---

## 3. Permission Verification & Onboarding

Helper logic to prompt the user to enable notification access via Android Settings:

```kotlin
package com.antigravity.expensetracker.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.antigravity.expensetracker.service.TransactionNotificationListener

object PermissionHelper {

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
```

---

## 4. Rule-Based Regex Parsing Engine

Create `TransactionParserEngine.kt`:

```kotlin
package com.antigravity.expensetracker.data.parser

import java.util.regex.Pattern

data class ParsedOutput(
    val amount: Double,
    val currency: String,
    val isDebit: Boolean,
    val counterparty: String,
    val timestamp: Long
)

class TransactionParserEngine {

    // Example pattern for: "Paid $25.50 to Starbucks" or "Debited USD 12.00 at Uber"
    private val standardDebitPattern = Pattern.compile(
        "(?i)(?:paid|spent|debited|charged)\\s+([A-Z$€£]{1,3})?\\s?([0-9]+(?:[.,][0-9]{2})?)\\s+(?:to|at)\\s+([^.,;\\n]+)"
    )

    // Example pattern for: "Received $500.00 from John" or "Credited USD 100.00"
    private val standardCreditPattern = Pattern.compile(
        "(?i)(?:received|credited|deposit)\\s+([A-Z$€£]{1,3})?\\s?([0-9]+(?:[.,][0-9]{2})?)\\s*(?:from\\s+([^.,;\\n]+))?"
    )

    fun parse(rawText: String, timestamp: Long): ParsedOutput? {
        // Test Debit
        val debitMatcher = standardDebitPattern.matcher(rawText)
        if (debitMatcher.find()) {
            val currency = debitMatcher.group(1) ?: "USD"
            val rawAmount = debitMatcher.group(2)?.replace(",", "") ?: "0.0"
            val counterparty = debitMatcher.group(3)?.trim().orEmpty()

            return ParsedOutput(
                amount = rawAmount.toDoubleOrNull() ?: 0.0,
                currency = currency,
                isDebit = true,
                counterparty = counterparty,
                timestamp = timestamp
            )
        }

        // Test Credit
        val creditMatcher = standardCreditPattern.matcher(rawText)
        if (creditMatcher.find()) {
            val currency = creditMatcher.group(1) ?: "USD"
            val rawAmount = creditMatcher.group(2)?.replace(",", "") ?: "0.0"
            val counterparty = creditMatcher.group(3)?.trim() ?: "Direct Deposit"

            return ParsedOutput(
                amount = rawAmount.toDoubleOrNull() ?: 0.0,
                currency = currency,
                isDebit = false,
                counterparty = counterparty,
                timestamp = timestamp
            )
        }

        return null
    }

    suspend fun processNotification(packageName: String, title: String, content: String, postTime: Long) {
        val targetText = "$title $content"
        val result = parse(targetText, postTime) ?: return

        // Persist to Room database & trigger interactive feedback notification
    }
}
```

---

## 5. Development Milestones for Agent Execution

1. **Milestone 1**: Implement `TransactionNotificationListener`, verify permission binding, and test payload extraction from mock notifications.
2. **Milestone 2**: Setup Room database with encrypted SQLite engine and define the core DAO operations.
3. **Milestone 3**: Build parser unit test suite covering sample bank notifications (English + local formats).
4. **Milestone 4**: Implement transfer deduplication window (3-minute buffer for equal debit/credit pairs).
5. **Milestone 5**: Build the user interface with Jetpack Compose (Transaction Feed, Category Mapper, Account Management).