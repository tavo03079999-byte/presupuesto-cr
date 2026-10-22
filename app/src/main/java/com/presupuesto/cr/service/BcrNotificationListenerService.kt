package com.presupuesto.cr.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.presupuesto.cr.PresupuestoApp
import com.presupuesto.cr.domain.usecase.ParseBcrNotificationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BcrNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var parseBcrNotificationUseCase: ParseBcrNotificationUseCase

    override fun onCreate() {
        super.onCreate()
        val app = application as PresupuestoApp
        parseBcrNotificationUseCase = ParseBcrNotificationUseCase(app.repository)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val combinedBody = if (bigText.isNotBlank()) bigText else text

        Log.d("BcrListener", "Notificación recibida de paquete: ${sbn.packageName} | Titulo: $title")

        serviceScope.launch {
            val app = application as PresupuestoApp
            val pendingEntity = parseBcrNotificationUseCase(title, combinedBody)

            if (pendingEntity != null) {
                Log.d("BcrListener", "¡Transacción BCR detectada con éxito! Monto: ₡${pendingEntity.parsedAmount}")
                app.repository.insertBcrPending(pendingEntity)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
