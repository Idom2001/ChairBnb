package com.example.chairbnb.classes.classHelper

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar


class DateManager(
    private val context: Context,
    private val dateSetListener: (Int, Int, Int) -> Unit
) {
    private val calendar = Calendar.getInstance()
    fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                dateSetListener(selectedYear, selectedMonth, selectedDay)
            }, year, month, day)
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    fun getDefaultHour(): Int {
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun getDefaultMinute(): Int {
        return calendar.get(Calendar.MINUTE)
    }

}