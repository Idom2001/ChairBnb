package com.example.chairbnb.classes.bookingRooms

data class RoomWithAvailableTime(
    val room: Room, val availableHoursFromNow: Double,
    val suggestedStartTime: String?, val suggestedDate: String? = null
)
