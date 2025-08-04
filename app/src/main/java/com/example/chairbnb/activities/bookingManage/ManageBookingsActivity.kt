package com.example.chairbnb.activities.bookingManage
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chairbnb.R
import com.example.chairbnb.classes.bookingRooms.Booking
import com.example.chairbnb.classes.bookingRooms.Room
import com.example.chairbnb.classes.bookingRooms.RoomWithAvailableTime
import com.example.chairbnb.classes.displaments.RoomAdapter
import com.example.chairbnb.objects.dataBase.AuthManager
import com.example.chairbnb.objects.dataBase.BookingStoreManager
import com.example.chairbnb.objects.dataBase.FireStoreManager
import com.example.chairbnb.objects.objectsHelper.TimeManager
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth

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
            roomsMap.clear()
            rooms.forEach { roomsMap[it.id] = it }
            BookingStoreManager.getUserBookings(userId, onSuccess = { bookings ->
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
                    }
                }
                if (displayedRooms.isEmpty()) {
                    probTextView.visibility = View.VISIBLE
                    probTextView.text = getString(R.string.no_active)
                } else {
                    probTextView.visibility = View.GONE
                }
                adapter.notifyDataSetChanged()
                bookings.filter { BookingStoreManager.isBookingExpired(it) }
                    .forEach { expiredBooking ->
                        BookingStoreManager.cancelBooking(expiredBooking, {}, {})
                    }
            }, onFailure = {})
        }, onFailure = {})
    }

    private fun onRoomClicked(roomWithAvailableTime: RoomWithAvailableTime) {
        val booking = BookingStoreManager.findBookingByRoom(
            roomWithAvailableTime.room.id,
            roomWithAvailableTime.suggestedStartTime,
            bookingsList
        )
        if (booking == null) {
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
                }, {})
            }

            .setNegativeButton("No", null)
            .show()
    }

    private fun formatHour(hour: Int): String {
        return TimeManager.formatTime(hour, 0)
    }
}
