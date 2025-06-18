package com.example.chairbnb.objects.dataBase

import com.example.chairbnb.classes.bookingRooms.Booking
import com.example.chairbnb.objects.objectsHelper.TimeManager
import com.google.firebase.firestore.FirebaseFirestore

object BookingStoreManager {
    private val firestore = FirebaseFirestore.getInstance()

    fun bookRoom(
        userId: String,
        roomId: String,
        date: String,
        startHour: Int,
        hoursCount: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bookingData = mapOf(
            "userId" to userId,
            "roomId" to roomId,
            "date" to date,
            "startHour" to startHour,
            "hoursCount" to hoursCount
        )
        firestore.collection("bookings").add(bookingData)
            .addOnSuccessListener {
                updateRoomAvailabilityAfterBooking(
                    roomId, date, startHour,
                    hoursCount, onSuccess, onFailure
                )
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getUserBookings(
        userId: String,
        onSuccess: (List<Booking>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val bookings = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        Booking(
                            id = doc.id,
                            userId = doc.getString("userId") ?: return@mapNotNull null,
                            roomId = doc.getString("roomId") ?: return@mapNotNull null,
                            date = doc.getString("date") ?: return@mapNotNull null,
                            startHour = (doc.getLong("startHour") ?: 0L).toInt(),
                            hoursCount = (doc.getLong("hoursCount") ?: 0L).toDouble()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filterNot { isBookingExpired(it) }

                onSuccess(bookings)
            }
            .addOnFailureListener { onFailure(it) }
    }


    fun cancelBooking(booking: Booking, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val bookingRef = firestore.collection("bookings").document(booking.id)
        val roomRef = firestore.collection("rooms").document(booking.roomId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val availableHoursRaw = snapshot.get("availableHours")
                    as? Map<String, List<Map<String, Long>>> ?: emptyMap()

            val currentRangesRaw = availableHoursRaw[booking.date]?.map {
                val start = it["start"] ?: 0L
                val end = it["end"] ?: 0L
                start.toInt() to end.toInt()
            }?.toMutableList() ?: mutableListOf()

            currentRangesRaw.add(booking.startHour to (booking.startHour + booking.hoursCount.toInt()))
            val mergedRanges = mergeTimeRanges(currentRangesRaw)

            val updatedRanges = mergedRanges.map { (start, end) ->
                mapOf("start" to start.toLong(), "end" to end.toLong())
            }

            val newAvailabilityMap = availableHoursRaw.toMutableMap()
            newAvailabilityMap[booking.date] = updatedRanges

            transaction.delete(bookingRef)
            transaction.update(roomRef, "availableHours", newAvailabilityMap)
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteExpiredBookings(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("bookings")
            .get()
            .addOnSuccessListener { result ->
                val batch = firestore.batch()
                val todayStr = TimeManager.getTodayDateString()
                val currentHour = TimeManager.getHourOfDay()

                result.documents.forEach { doc ->
                    try {
                        val date = doc.getString("date") ?: return@forEach
                        val startHour = (doc.getLong("startHour") ?: 0L).toInt()
                        val hoursCount = (doc.getLong("hoursCount") ?: 0L).toInt()

                        val isExpired =
                            date < todayStr || (date == todayStr && currentHour >= startHour + hoursCount)

                        if (isExpired) {
                            batch.delete(doc.reference)
                        }
                    } catch (_: Exception) {
                    }
                }

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun findBookingByRoom(
        roomId: String, suggestedStartTime: String?,
        bookingsList: MutableList<Booking>
    ): Booking? {
        if (suggestedStartTime == null) return null
        val hour = TimeManager.parseHour(suggestedStartTime)
        return bookingsList.find { it.roomId == roomId && it.startHour == hour }
    }

    fun isBookingExpired(booking: Booking): Boolean {
        val todayStr = TimeManager.getTodayDateString()
        if (booking.date < todayStr) return true
        if (booking.date > todayStr) return false
        val currentHour = TimeManager.getHourOfDay()
        return currentHour >= booking.startHour + booking.hoursCount
    }


    private fun mergeTimeRanges(ranges: MutableList<Pair<Int, Int>>): List<Pair<Int, Int>> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val merged = mutableListOf<Pair<Int, Int>>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.second) {
                current = current.first to maxOf(current.second, next.second)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun updateRoomAvailabilityAfterBooking(
        roomId: String,
        date: String,
        startHour: Int,
        hoursCount: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val roomDocRef = firestore.collection("rooms").document(roomId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomDocRef)
            val availableHoursRaw = snapshot.get("availableHours")
                    as? Map<String, List<Map<String, Long>>> ?: emptyMap()

            val bookedStart = startHour
            val bookedEnd = startHour + hoursCount

            val currentRanges = availableHoursRaw[date] ?: emptyList()
            val updatedRanges = mutableListOf<Map<String, Int>>()

            for (range in currentRanges) {
                val start = range["start"]?.toInt() ?: continue
                val end = range["end"]?.toInt() ?: continue

                when {
                    bookedEnd <= start || bookedStart >= end -> {
                        updatedRanges.add(mapOf("start" to start, "end" to end))
                    }

                    bookedStart > start && bookedEnd < end -> {
                        updatedRanges.add(mapOf("start" to start, "end" to bookedStart))
                        updatedRanges.add(mapOf("start" to bookedEnd, "end" to end))
                    }

                    bookedStart <= start && bookedEnd in (start + 1)..end -> {
                        updatedRanges.add(mapOf("start" to bookedEnd, "end" to end))
                    }

                    bookedStart in start until end && bookedEnd >= end -> {
                        updatedRanges.add(mapOf("start" to start, "end" to bookedStart))
                    }
                }
            }

            val newAvailableHours = availableHoursRaw.toMutableMap()
            if (updatedRanges.isEmpty()) newAvailableHours.remove(date)
            else newAvailableHours[date] = updatedRanges.map {
                mapOf(
                    "start" to it["start"]!!.toLong(),
                    "end" to it["end"]!!.toLong()
                )
            }

            transaction.update(roomDocRef, "availableHours", newAvailableHours)
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
