package com.example.chairbnb.objects.objectsHelper

import android.annotation.SuppressLint
import com.example.chairbnb.classes.classHelper.Constants
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeManager {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    @SuppressLint("ConstantLocale")
    private val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private fun shouldMoveToNextDay(
        selectedDate: String,
        selectedHour: Int,
        selectedMinute: Int
    ): Boolean {
        val selectedDateTime = LocalDateTime.of(
            LocalDate.parse(selectedDate, dateFormatter),
            LocalTime.of(selectedHour, selectedMinute)
        )
        return selectedDateTime.isBefore(LocalDateTime.now())
    }

    private fun getNextDate(date: String): String {
        val parsedDate = LocalDate.parse(date, dateFormatter)
        return parsedDate.plusDays(1).format(dateFormatter)
    }

    fun adjustDateIfNeeded(
        selectedDate: String, selectedHour: Int,
        selectedMinute: Int
    ): String {
        return if (shouldMoveToNextDay(selectedDate, selectedHour, selectedMinute)) {
            getNextDate(selectedDate)
        } else {
            selectedDate
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val h = hour % Constants.Hour.HOURS_IN_DAY
        return String.format("%02d:%02d", h, minute)
    }

    fun getHourOfDay(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    fun getTodayDateString(): String {
        return formatDateToString(Date())
    }

    fun formatDate(year: Int, month: Int, day: Int): String {
        val localDate = LocalDate.of(year, month + 1, day)
        return localDate.format(dateFormatter)
    }

    fun parseHour(timeStr: String): Int {
        return getParts(timeStr).getOrNull(0)?.toIntOrNull() ?: 0

    }

    fun parseHourByCheck(timeStr: String): Int? {
        return getParts(timeStr).firstOrNull()?.toIntOrNull()
    }

    private fun getParts(timeStr: String): List<String> {
        return timeStr.split(":")
    }

    fun getHour(timeStr: String): Int? {
        return getParts(timeStr).getOrNull(0)?.toIntOrNull()
    }

    fun getMinutes(timeStr: String): Int {
        return getParts(timeStr).getOrNull(1)?.toIntOrNull() ?: 0
    }

    fun calculateEndTime(startHour: Int, startMinute: Int, durationHours: Double): Pair<Int, Int> {
        val totalMinutes = startHour * Constants.Hour.MINUETS_IN_HOUR + startMinute +
                (durationHours * Constants.Hour.MINUETS_IN_HOUR).toInt()
        val endHour = totalMinutes / Constants.Hour.MINUETS_IN_HOUR
        val endMinute = totalMinutes % Constants.Hour.MINUETS_IN_HOUR
        return Pair(endHour % Constants.Hour.HOURS_IN_DAY, endMinute)
    }
    fun hasFutureOrTodayDates(dates: List<String>): Boolean {
        val today = LocalDate.now()
        return dates.any { dateStr ->
            try {
                val date = LocalDate.parse(dateStr, dateFormatter)
                !date.isBefore(today)  // date >= today its good
            } catch (e: Exception) {
                false
            }
        }
    }


    fun formatDateToString(date: Date): String {
        return simpleDateFormat.format(date)
    }

    fun parseDateOrNull(dateStr: String): Date? {
        return try {
            simpleDateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
}