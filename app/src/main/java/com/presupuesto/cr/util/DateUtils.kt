package com.presupuesto.cr.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val standardDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR"))
    private val friendlyDateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CR"))
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale("es", "CR"))
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale("es", "CR"))

    fun formatDate(timestamp: Long): String = standardDateFormat.format(Date(timestamp))
    fun formatFriendly(timestamp: Long): String = friendlyDateFormat.format(Date(timestamp))
    fun formatShort(timestamp: Long): String = shortDateFormat.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * Calcula los días restantes en la quincena actual (1 al 15 o 16 al fin de mes)
     * muy común en el ciclo salarial costarricense.
     */
    fun getRemainingDaysInCurrentFortnight(): Int {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (currentDay <= 15) {
            15 - currentDay + 1
        } else {
            maxDayOfMonth - currentDay + 1
        }
    }

    /**
     * Obtiene el final del periodo quincenal actual en milisegundos
     */
    fun getEndOfCurrentFortnight(): Long {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        if (currentDay <= 15) {
            calendar.set(Calendar.DAY_OF_MONTH, 15)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.timeInMillis
    }
}
