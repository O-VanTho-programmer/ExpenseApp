package com.antigravity.expensetracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.antigravity.expensetracker.ExpenseTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            try {
                val app = applicationContext as? ExpenseTrackerApp ?: ExpenseTrackerApp.instance
                app.transactionRepository.processNotification(
                    packageName = packageName,
                    title = title,
                    content = fullContent,
                    postTime = sbn.postTime
                )
            } catch (e: Exception) {
                // Silent local handling to protect privacy
            }
        }
    }
}
