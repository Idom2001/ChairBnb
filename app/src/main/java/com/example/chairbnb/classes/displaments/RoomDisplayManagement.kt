package com.example.chairbnb.Classes.Displaments

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chairbnb.Classes.BookinRooms.Room
import com.example.chairbnb.Classes.BookinRooms.RoomWithAvailableTime
import com.example.chairbnb.Classes.ClassHelper.Constants
import com.example.chairbnb.Objects.ObjectsHelper.RoomAvailability
import com.example.chairbnb.Objects.ObjectsHelper.TimeManager
import com.google.android.material.textview.MaterialTextView

class RoomDisplayManagement(
    private val context: Context,
    private val rooms: List<Room>,
    private val selectedDate: String,
    private val selectedTime: String,
    private val selectedEquipments: List<String>,
    private val roomRecyclerView: RecyclerView,
    private val probTextView: MaterialTextView,
    private val onRoomSelected: (Room, actualStartHour: String, Double) -> Unit
) {
    private val selectedHour: Double
    private val selectedMinutes: Double
    private val selectedTimeAsDouble: Double

    init {
        val timeParts = selectedTime.split(":")
        selectedHour = timeParts[0].toDouble()
        selectedMinutes = timeParts[1].toDouble()
        selectedTimeAsDouble = selectedHour + selectedMinutes / Constants.Hour.MINUETS_IN_HOUR
    }

    fun display() {
        var availableRooms = filterAvailableRoomsOnSelectedTime()
        var displayDate = selectedDate

        if (availableRooms.isEmpty()) {
            availableRooms = findAlternativeRooms().also { result ->
                if (result.isNotEmpty()) {
                    probTextView.text =
                        "No rooms available at $selectedTime. Showing rooms available later today" +
                                " ($selectedDate)"
                    probTextView.visibility = View.VISIBLE
                }
            }

            if (availableRooms.isEmpty()) {
                val closestDate = RoomAvailability.findClosestAvailableDate(rooms, selectedDate)
                if (closestDate != null) {
                    displayDate = closestDate
                    availableRooms = filterRoomsOnClosestDate(closestDate)
                    if (availableRooms.isNotEmpty()) {
                        probTextView.text =
                            "No rooms available on $selectedDate. Showing rooms available on" +
                                    " $closestDate"
                        probTextView.visibility = View.VISIBLE
                    }
                }
            }
        }

        val roomsWithTimeLeft = buildRoomWithAvailableTimeList(availableRooms)

        if (roomsWithTimeLeft.isEmpty()) {
            probTextView.text = "No rooms available"
            probTextView.visibility = View.VISIBLE
        }

        displayRoomList(roomsWithTimeLeft)
    }

    private fun filterAvailableRoomsOnSelectedTime(): List<Room> {
        return rooms.filter { room ->
            room.availableDates.contains(selectedDate) &&
                    (selectedEquipments.isEmpty() || selectedEquipments.all {
                        room.equipments.contains(
                            it
                        )
                    }) &&
                    RoomAvailability.isRoomAvailableAtTime(room, selectedDate, selectedTime)
        }.filter {
            it.getAvailableTime(
                selectedHour,
                selectedMinutes,
                selectedDate
            ) >= Constants.LibTime.MIN_TIME_ROOM
        }
    }

    private fun findAlternativeRooms(): List<Room> {
        return rooms.filter { room ->
            room.availableDates.contains(selectedDate) &&
                    (selectedEquipments.isEmpty() || selectedEquipments.any {
                        room.equipments.contains(
                            it
                        )
                    }) &&
                    RoomAvailability.hasNextAvailableHourWithAtLeastOneHour(
                        room,
                        selectedDate,
                        selectedTimeAsDouble
                    )
        }
    }

    private fun filterRoomsOnClosestDate(date: String): List<Room> {
        return rooms.filter { room ->
            room.availableDates.contains(date) &&
                    (selectedEquipments.isEmpty() || selectedEquipments.any {
                        room.equipments.contains(
                            it
                        )
                    }) &&
                    (room.availableHours[date]?.any { (start, end) -> end - start >= Constants.LibTime.MIN_TIME_ROOM }
                        ?: false)
        }
    }


    private fun buildRoomWithAvailableTimeList(rooms: List<Room>): List<RoomWithAvailableTime> {
        return rooms.mapNotNull { room ->
            val availableTime = room.getAvailableTime(
                selectedHour,
                selectedMinutes, selectedDate
            )

            if (availableTime >= Constants.LibTime.MIN_TIME_ROOM) {
                RoomWithAvailableTime(room, availableTime, selectedTime)
            } else {
                findNextAvailableTime(room)
            }
        }
    }

    private fun findNextAvailableTime(room: Room): RoomWithAvailableTime? {
        val hoursForDate = room.availableHours[selectedDate] ?: return null
        val nextStart = hoursForDate
            .map { it.first }
            .filter { it > selectedTimeAsDouble }
            .firstOrNull { start ->
                val range = hoursForDate.find { it.first == start }
                range != null && range.second - start >= Constants.LibTime.MIN_TIME_ROOM
            }

        return nextStart?.let {
            val hoursPart = it.toInt()
            val minutesPart = ((it - hoursPart) * Constants.Hour.MINUETS_IN_HOUR).toInt()
            val formattedStart = TimeManager.formatTime(hoursPart, minutesPart)
            val newAvailableTime = room.getAvailableTime(
                hoursPart.toDouble(),
                minutesPart.toDouble(), selectedDate
            )
            if (newAvailableTime >= Constants.LibTime.MIN_TIME_ROOM) {
                RoomWithAvailableTime(room, newAvailableTime, formattedStart)
            } else null
        }
    }

    private fun displayRoomList(rooms: List<RoomWithAvailableTime>) {
        roomRecyclerView.layoutManager = LinearLayoutManager(context)
        roomRecyclerView.adapter = RoomAdapter(rooms) { roomWithTime ->
            onRoomSelected(
                roomWithTime.room,
                roomWithTime.suggestedStartTime.toString(),
                roomWithTime.availableHoursFromNow
            )
        }
    }
}
