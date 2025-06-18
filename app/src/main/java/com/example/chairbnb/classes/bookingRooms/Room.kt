package com.example.chairbnb.Classes.BookinRooms

import com.example.chairbnb.Classes.ClassHelper.Constants

data class Room(
    val id: String,
    val name: String,
    val description: String,
    val equipments: List<String>,
    val maxParticipants: Int,
    val availableDates: List<String>,
    val availableHours: Map<String, List<Pair<Int, Int>>>
) {
    fun getAvailableTime(startHour: Double, startMinute: Double, date: String): Double {
        val startTime = startHour + startMinute / Constants.Hour.MINUETS_IN_HOUR
        val dailyHours = availableHours[date] ?: return Constants.Hour.DEFAULT
        for ((start, end) in dailyHours) {
            if (startTime >= start && startTime < end) {
                return String.format(
                    "%.2f",
                    minOf(Constants.LibTime.MAX_TIME_ROOM_DOUBLE, end - startTime)
                ).toDouble()
            }
        }
        return Constants.Hour.DEFAULT
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "equipments" to equipments,
            "maxParticipants" to maxParticipants,
            "availableDates" to availableDates,
            "availableHours" to availableHours.mapValues { entry ->
                entry.value.map { pair ->
                    mapOf("start" to pair.first, "end" to pair.second)
                }
            }
        )
    }
}