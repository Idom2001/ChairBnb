package com.example.chairbnb.Classes.BookinRooms

data class Booking(
    val id: String, val userId: String, val roomId: String, val date: String, val startHour: Int,
    val hoursCount: Double
)

