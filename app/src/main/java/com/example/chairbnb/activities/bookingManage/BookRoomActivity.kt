package com.example.chairbnb.activities.bookingManage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.chairbnb.R
import com.example.chairbnb.activities.MainActivity
import com.example.chairbnb.classes.bookingRooms.Room
import com.example.chairbnb.classes.classHelper.Constants
import com.example.chairbnb.classes.displaments.RoomDisplayManagement
import com.example.chairbnb.objects.dataBase.AuthManager
import com.example.chairbnb.objects.dataBase.BookingStoreManager
import com.example.chairbnb.objects.dataBase.FireStoreManager
import com.example.chairbnb.objects.objectsHelper.TimeManager
import com.google.android.material.textview.MaterialTextView

class BookRoomActivity : AppCompatActivity() {
    private lateinit var roomRecyclerView: RecyclerView
    private lateinit var probTextView: MaterialTextView
    private var rooms: List<Room> = emptyList()
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedEquipments: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_book_room)
        findViews()
        uploadRoomsFirstTime()
    }

    private fun findViews() {
        selectedDate = intent.getStringExtra("selectedDate") ?: ""
        selectedTime = intent.getStringExtra("selectedTime") ?: ""
        selectedEquipments = intent.getStringArrayListExtra("selectedEquipments") ?: emptyList()
        roomRecyclerView = findViewById(R.id.roomsRecyclerView)
        probTextView = findViewById(R.id.ProbTextView)
    }

    private fun uploadRoomsFirstTime() {
        FireStoreManager.getRooms(
            onSuccess = { existingRooms ->
                if (existingRooms.isNotEmpty()) {
                    rooms = existingRooms
                    displayRooms()
                } else {
                    try {
                        val jsonString =
                            assets.open("rooms.json").bufferedReader().use { it.readText() }
                        FireStoreManager.uploadInitialRoomsData(
                            jsonString,
                            onSuccess = {
                                loadRoomsFromFireStore()
                            },
                            onFailure = {}
                        )
                    } catch (e: Exception) {
                        Log.e("uploadRoomsFirstTime", "Error reading JSON", e)
                    }
                }
            },
            onFailure = {}
        )
    }

    private fun loadRoomsFromFireStore() {
        FireStoreManager.getRooms(
            onSuccess = { fetchedRooms ->
                rooms = fetchedRooms
                displayRooms()
            },
            onFailure = {}
        )
    }

    private fun displayRooms() {
        val displayer = RoomDisplayManagement(
            context = this,
            rooms = rooms,
            selectedDate = selectedDate,
            selectedTime = selectedTime,
            selectedEquipments = selectedEquipments,
            roomRecyclerView = roomRecyclerView,
            probTextView = probTextView,
            onRoomSelected = this::onRoomSelected
        )
        displayer.display()
    }

    private fun onRoomSelected(room: Room, actualStartHour: String, availableHours: Double) {

        if (!AuthManager.isUserIn()) {
            Toast.makeText(this, "You need to sign in before booking", Toast.LENGTH_SHORT).show()
            return
        }

        val startHour = TimeManager.parseHourByCheck(actualStartHour)
        if (startHour == null) {
            Toast.makeText(this, "Invalid start time", Toast.LENGTH_SHORT).show()
            return
        }

        val hoursCount = availableHours.toInt().coerceAtMost(Constants.LibTime.MAX_TIME_ROOM_INT)
        BookingStoreManager.bookRoom(
            userId = AuthManager.currentUserUid()!!,
            roomId = room.id,
            date = selectedDate,
            startHour = startHour,
            hoursCount = hoursCount,
            onSuccess = {
                Toast.makeText(this, "Room booked successfully!", Toast.LENGTH_SHORT).show()
                loadRoomsFromFireStore()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            onFailure = {}
        )
    }
}
