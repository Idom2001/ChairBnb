package com.example.chairbnb.Activities.BookingManage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.chairbnb.Activities.MainActivity
import com.example.chairbnb.Classes.BookinRooms.Room
import com.example.chairbnb.Classes.ClassHelper.Constants
import com.example.chairbnb.Classes.Displaments.RoomDisplayManagement
import com.example.chairbnb.Objects.DataBase.AuthManager
import com.example.chairbnb.Objects.DataBase.BookingStoreManager
import com.example.chairbnb.Objects.DataBase.FireStoreManager
import com.example.chairbnb.Objects.ObjectsHelper.TimeManager
import com.example.chairbnb.R
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
        Log.d("BookRoomActivity", "Received Equipments: $selectedEquipments")
        roomRecyclerView = findViewById(R.id.roomsRecyclerView)
        probTextView = findViewById(R.id.ProbTextView)
    }

    private fun uploadRoomsFirstTime() {
        FireStoreManager.getRooms(
            onSuccess = { existingRooms ->
                if (existingRooms.isNotEmpty()) {
                    Toast.makeText(this, "Rooms already exist, loading...", Toast.LENGTH_SHORT)
                        .show()
                    rooms = existingRooms
                    displayRooms()
                } else {
                    try {
                        val jsonString =
                            assets.open("rooms.json").bufferedReader().use { it.readText() }
                        FireStoreManager.uploadInitialRoomsData(
                            jsonString,
                            onSuccess = {
                                Toast.makeText(
                                    this,
                                    "Rooms uploaded successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadRoomsFromFireStore()
                            },
                            onFailure = { e ->
                                Toast.makeText(
                                    this,
                                    "Upload failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.d("upload", "Upload failed: ${e.message}")
                            }
                        )
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error reading JSON: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            },
            onFailure = { e ->
                Toast.makeText(this, "Failed to check rooms: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        )
    }

    private fun loadRoomsFromFireStore() {
        FireStoreManager.getRooms(
            onSuccess = { fetchedRooms ->
                rooms = fetchedRooms
                displayRooms()
            },
            onFailure = { e ->
                Toast.makeText(this, "Failed to load rooms: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
