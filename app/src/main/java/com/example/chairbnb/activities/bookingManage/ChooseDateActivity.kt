package com.example.chairbnb.activities.bookingManage
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.chairbnb.R
import com.example.chairbnb.classes.classHelper.DateManager
import com.example.chairbnb.objects.objectsHelper.TimeManager
import com.google.android.material.textview.MaterialTextView

class ChooseDateActivity : AppCompatActivity() {

    private lateinit var selectDateButton: Button
    private lateinit var selectedDateTextView: MaterialTextView
    private lateinit var proceedButton: Button
    private lateinit var checkboxComputer: CheckBox
    private lateinit var checkboxWhiteboard: CheckBox
    private lateinit var checkboxSpeakers: CheckBox
    private lateinit var checkboxChargingSpots: CheckBox
    private lateinit var timePicker: TimePicker
    private lateinit var datePickerHelper: DateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_date)
        findViews()
        selectDateButton.setOnClickListener { datePickerHelper.showDatePickerDialog() }
        proceedButton.setOnClickListener { proceedToRoomSelection() }
        setupTimePickerListener()
        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupTimePickerListener() {
        timePicker.setOnTimeChangedListener { _, hour, minute ->
            val currentSelectedDate = selectedDateTextView.text.toString()
            if (currentSelectedDate.isNotEmpty() && currentSelectedDate != "Select a date and hour") {
                val newDate = TimeManager.adjustDateIfNeeded(currentSelectedDate, hour, minute)
                if (newDate != currentSelectedDate) {
                    selectedDateTextView.text = newDate
                    Toast.makeText(
                        this,
                        "Date adjusted to next day because time passed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun findViews() {
        selectDateButton = findViewById(R.id.selectDateButton)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        proceedButton = findViewById(R.id.proceedButton)
        checkboxComputer = findViewById(R.id.checkBoxComputer)
        checkboxWhiteboard = findViewById(R.id.checkBoxWhiteboard)
        checkboxSpeakers = findViewById(R.id.checkBoxSpeakers)
        checkboxChargingSpots = findViewById(R.id.checkBoxCharging)
        timePicker = findViewById(R.id.timePicker)
        datePickerHelper = DateManager(this) { year, month, day ->
            val formatted = TimeManager.formatDate(year, month, day)
            selectedDateTextView.text = formatted
        }
        val defaultHour = datePickerHelper.getDefaultHour()
        val defaultMinute = datePickerHelper.getDefaultMinute()
        timePicker.hour = defaultHour
        timePicker.minute = defaultMinute
    }

    private fun proceedToRoomSelection() {
        val selectedDate = selectedDateTextView.text.toString()

        if (selectedDate.isEmpty() || selectedDate == "Select a date and hour") {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        val hour = timePicker.hour
        val minute = timePicker.minute
        val selectedTime = TimeManager.formatTime(hour, minute)
        val selectedEquipments = getSelectedEquipments()
        val intent = Intent(this, BookRoomActivity::class.java)
        intent.putExtra("selectedDate", selectedDate)
        intent.putExtra("selectedTime", selectedTime)
        intent.putStringArrayListExtra("selectedEquipments", ArrayList(selectedEquipments))
        startActivity(intent)
    }

    private fun getSelectedEquipments(): List<String> {
        val selectedEquipments = mutableListOf<String>()
        if (checkboxComputer.isChecked) selectedEquipments.add("Computer")
        if (checkboxWhiteboard.isChecked) selectedEquipments.add("Whiteboard")
        if (checkboxSpeakers.isChecked) selectedEquipments.add("Speakers")
        if (checkboxChargingSpots.isChecked) selectedEquipments.add("Charging Spots")
        return selectedEquipments
    }
}

