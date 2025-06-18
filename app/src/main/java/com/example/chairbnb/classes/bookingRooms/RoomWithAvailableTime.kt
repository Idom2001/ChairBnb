package com.example.chairbnb.Classes.BookinRooms

data class RoomWithAvailableTime(
    val room: Room, val availableHoursFromNow: Double,
    val suggestedStartTime: String?, val suggestedDate: String? = null
)
