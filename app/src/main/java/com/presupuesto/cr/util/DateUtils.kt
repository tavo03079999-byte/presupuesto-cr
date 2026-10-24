package com.presupuesto.cr.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val standardDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR"))
    private val friendlyDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "CR"))
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale("es", "CR"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "CR"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "CR"))

    fun formatDate(timestamp: Long): String = standardDateFormat.format(Date(timestamp))
    fun formatFriendly(timestamp: Long): String = friendlyDateFormat.format(Date(timestamp))
    fun formatShort(timestamp: Long): String = shortDateFormat.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))
    fun formatMonthYear(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return monthYearFormat.format(cal.time).replaceFirstChar { it.uppercase() }
    }

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

    fun addDaysToStartOfDay(baseTimestamp: Long, deltaDays: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseTimestamp
            add(Calendar.DAY_OF_YEAR, deltaDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun addDaysToEndOfDay(baseTimestamp: Long, deltaDays: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseTimestamp
            add(Calendar.DAY_OF_YEAR, deltaDays)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun utcDatePickerMillisToLocalStartOfDay(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val localCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    fun localMillisToUtcDatePickerMillis(localMillis: Long): Long {
        val localCal = Calendar.getInstance().apply {
            timeInMillis = localMillis
        }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                localCal.get(Calendar.YEAR),
                localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }
        return utcCal.timeInMillis
    }

    fun getCurrentMonthBounds(): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    fun getPreviousMonthBounds(): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    fun getCurrentWeekBounds(): Pair<Long, Long> {
        val endCal = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return startCal.timeInMillis to getEndOfDay(endCal.timeInMillis)
    }

    fun getPreviousWeekBounds(): Pair<Long, Long> {
        val endCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }
        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -13)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return startCal.timeInMillis to getEndOfDay(endCal.timeInMillis)
    }

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
