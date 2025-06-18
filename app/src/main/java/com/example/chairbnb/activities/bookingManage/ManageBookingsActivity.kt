package com.example.chairbnb.Activities.BookingManage

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chairbnb.Classes.BookinRooms.Booking
import com.example.chairbnb.Classes.BookinRooms.Room
import com.example.chairbnb.Classes.BookinRooms.RoomWithAvailableTime
import com.example.chairbnb.Classes.Displaments.RoomAdapter
import com.example.chairbnb.Objects.DataBase.AuthManager
import com.example.chairbnb.Objects.DataBase.BookingStoreManager
import com.example.chairbnb.Objects.DataBase.FireStoreManager
import com.example.chairbnb.Objects.ObjectsHelper.TimeManager
import com.example.chairbnb.R
import com.google.android.material.textview.MaterialTextView

class ManageBookingsActivity : AppCompatActivity() {
    private val bookingsList = mutableListOf<Booking>()
    private val roomsMap = mutableMapOf<String, Room>()
    private val displayedRooms = mutableListOf<RoomWithAvailableTime>()
    private lateinit var adapter: RoomAdapter
    private lateinit var roomRecyclerView: RecyclerView
    private lateinit var probTextView: MaterialTextView
    private lateinit var userId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_bookings)
        findViews()
        setupRecyclerView()
        loadRoomsAndBookings()
    }

    private fun findViews() {
        roomRecyclerView = findViewById(R.id.roomsRecyclerView)
        probTextView = findViewById(R.id.ProbTextView)
        userId = AuthManager.currentUserUid().toString()
    }

    private fun setupRecyclerView() {
        adapter = RoomAdapter(
            displayedRooms, isForBooking = false,
            onBookingCancelConfirmed = { roomWithAvailableTime ->
                onRoomClicked(roomWithAvailableTime)
            }
        )
        roomRecyclerView.layoutManager = LinearLayoutManager(this)
        roomRecyclerView.adapter = adapter
    }

    private fun loadRoomsAndBookings() {
        FireStoreManager.getRooms(onSuccess = { rooms ->
            println("Rooms loaded: ${rooms.size}")
            roomsMap.clear()
            rooms.forEach { roomsMap[it.id] = it }
            BookingStoreManager.getUserBookings(userId, onSuccess = { bookings ->
                println("User bookings loaded: ${bookings.size}")
                bookingsList.clear()
                // clear out dated bookings
                val validBookings =
                    bookings.filter { booking -> !BookingStoreManager.isBookingExpired(booking) }
                bookingsList.addAll(validBookings)
                displayedRooms.clear()
                validBookings.forEach { booking ->
                    val room = roomsMap[booking.roomId]
                    if (room != null) {
                        displayedRooms.add(
                            RoomWithAvailableTime(
                                room = room,
                                suggestedStartTime = formatHour(booking.startHour),
                                availableHoursFromNow = booking.hoursCount,
                                suggestedDate = booking.date
                            )
                        )
                    } else println("Room not found for booking with roomId: ${booking.roomId}")
                }
                if (displayedRooms.isEmpty()) {
                    probTextView.visibility = View.VISIBLE
                    probTextView.text = "No active bookings"
                } else {
                    probTextView.visibility = View.GONE
                }
                adapter.notifyDataSetChanged()
                bookings.filter { BookingStoreManager.isBookingExpired(it) }
                    .forEach { expiredBooking ->
                        BookingStoreManager.cancelBooking(expiredBooking, {}, {})
                    }
            }, onFailure = {
                Toast.makeText(this, "Failed to load bookings", Toast.LENGTH_SHORT).show()
            })
        }, onFailure = {
            Toast.makeText(this, "Failed to load rooms", Toast.LENGTH_SHORT).show()
        })
    }

    private fun onRoomClicked(roomWithAvailableTime: RoomWithAvailableTime) {
        val booking = BookingStoreManager.findBookingByRoom(
            roomWithAvailableTime.room.id,
            roomWithAvailableTime.suggestedStartTime,
            bookingsList
        )
        if (booking == null) {
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage(
                "Are you sure you want to cancel the booking for room ${roomWithAvailableTime.room.name} on ${booking.date} at ${
                    formatHour(
                        booking.startHour
                    )
                }?"
            )
            .setPositiveButton("Yes") { _, _ ->
                BookingStoreManager.cancelBooking(booking, {
                    Toast.makeText(this, "Booking canceled", Toast.LENGTH_SHORT).show()
                    loadRoomsAndBookings()
                }, { e ->
                    Toast.makeText(this, "Failed to cancel: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("DEBUG", "Failed to cancel: ${e.message}")
                })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun formatHour(hour: Int): String {
        return TimeManager.formatTime(hour, 0)
    }
}
