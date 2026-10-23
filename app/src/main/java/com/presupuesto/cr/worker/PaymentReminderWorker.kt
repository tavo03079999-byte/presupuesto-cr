package com.presupuesto.cr.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.presupuesto.cr.R
import com.presupuesto.cr.util.CurrencyFormatter
import java.util.concurrent.TimeUnit

class PaymentReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val customTitle = inputData.getString(KEY_CUSTOM_TITLE)
        val customBody = inputData.getString(KEY_CUSTOM_BODY)
        val reminderId = inputData.getLong(KEY_REMINDER_ID, 1L).toInt()

        if (!customTitle.isNullOrBlank() && !customBody.isNullOrBlank()) {
            showNotification(reminderId, customTitle, customBody)
        } else {
            val reminderName = inputData.getString(KEY_REMINDER_NAME) ?: "Recordatorio de Pago"
            val reminderAmount = inputData.getDouble(KEY_REMINDER_AMOUNT, 0.0)
            showNotification(
                id = reminderId,
                title = "Pago próximo: $reminderName",
                body = "Monto programado: ${CurrencyFormatter.formatCrc(reminderAmount)}"
            )
        }
        return Result.success()
    }

    private fun showNotification(id: Int, title: String, body: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas y Recordatorios PresupuestoCR",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de presupuestos superados y recordatorios de pagos"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "presupuesto_cr_reminders"
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_REMINDER_NAME = "reminder_name"
        const val KEY_REMINDER_AMOUNT = "reminder_amount"
        const val KEY_CUSTOM_TITLE = "custom_title"
        const val KEY_CUSTOM_BODY = "custom_body"

        fun scheduleReminder(
            context: Context,
            reminderId: Long,
            name: String,
            amount: Double,
            dueDateMillis: Long,
            offsetHours: Int
        ) {
            val triggerTimeMillis = dueDateMillis - TimeUnit.HOURS.toMillis(offsetHours.toLong())
            val delayMillis = (triggerTimeMillis - System.currentTimeMillis()).coerceAtLeast(3000L)

            val data = Data.Builder()
                .putLong(KEY_REMINDER_ID, reminderId)
                .putString(KEY_REMINDER_NAME, name)
                .putDouble(KEY_REMINDER_AMOUNT, amount)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_$reminderId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelReminder(context: Context, reminderId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
        }

        fun triggerBudgetExceededNotification(
            context: Context,
            categoryId: Long,
            categoryName: String,
            spentAmount: Double,
            limitAmount: Double
        ) {
            val data = Data.Builder()
                .putLong(KEY_REMINDER_ID, 10000L + categoryId)
                .putString(KEY_CUSTOM_TITLE, "¡Presupuesto superado en $categoryName!")
                .putString(
                    KEY_CUSTOM_BODY,
                    "Has gastado ${CurrencyFormatter.formatCrc(spentAmount)} de tu límite de ${CurrencyFormatter.formatCrc(limitAmount)} (100%+)."
                )
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "budget_exceeded_$categoryId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
