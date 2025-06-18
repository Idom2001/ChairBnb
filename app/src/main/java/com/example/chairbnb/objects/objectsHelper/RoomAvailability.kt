package com.example.chairbnb.objects.objectsHelper

import com.example.chairbnb.classes.bookingRooms.Room
import java.text.SimpleDateFormat
import java.util.Locale

object RoomAvailability {

    fun isRoomAvailableAtTime(room: Room, selectedDate: String, selectedTime: String): Boolean {
        val hoursForDate = room.availableHours[selectedDate] ?: return false
        val selectedHour = selectedTime.split(":")[0].toInt()
        return hoursForDate.any { (start, end) ->
            selectedHour in start until end && (end - selectedHour) > 0
        }
    }

    fun hasNextAvailableHourWithAtLeastOneHour(
        room: Room,
        selectedDate: String,
        selectedTimeAsDouble: Double
    ): Boolean {
        val hoursForDate = room.availableHours[selectedDate] ?: return false
        return hoursForDate.any { (start, end) ->
            start > selectedTimeAsDouble && (end - start) >= 1.0
        }
    }

    fun findClosestAvailableDate(rooms: List<Room>, fromDate: String): String? {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val requestedDate = sdf.parse(fromDate) ?: return null

        val allAvailableDates = rooms.flatMap { it.availableDates }.distinct()
        val sortedDates = allAvailableDates.mapNotNull {
            val date = sdf.parse(it)
            if (date != null && date.after(requestedDate)) date else null
        }.sorted()

        return sortedDates.firstOrNull()?.let { sdf.format(it) }
    }
}
