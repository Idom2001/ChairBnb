package com.example.chairbnb.objects.dataBase

import android.annotation.SuppressLint
import com.example.chairbnb.classes.bookingRooms.Room
import com.example.chairbnb.objects.objectsHelper.TimeManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FireStoreManager {
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    fun saveUserData(
        userId: String,
        data: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("users").document(userId).set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getUserFullName(userId: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val fullName = doc.getString("fullName") ?: "User"
                onSuccess(fullName)
            }
            .addOnFailureListener { onFailure() }
    }

    private fun saveRooms(
        rooms: List<Room>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val batch = firestore.batch()
        val collectionRef = firestore.collection("rooms")
        rooms.forEach { room ->
            batch.set(collectionRef.document(room.id), room.toMap())
        }
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getRooms(onSuccess: (List<Room>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("rooms").get()
            .addOnSuccessListener { result ->
                val batch = firestore.batch()
                val collectionRef = firestore.collection("rooms")
                val rooms = result.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        val id = data["id"] as? String ?: return@mapNotNull null
                        val name = data["name"] as? String ?: ""
                        val description = data["description"] as? String ?: ""
                        val equipments = data["equipments"] as? List<String> ?: emptyList()
                        val maxParticipants = (data["maxParticipants"] as? Long)?.toInt() ?: 0
                        val availableDates = data["availableDates"] as? List<String> ?: emptyList()

                        val rawAvailableHours =
                            data["availableHours"] as? Map<String, List<Map<String, Long>>>
                                ?: emptyMap()
                        val availableHours = rawAvailableHours.mapValues { entry ->
                            entry.value.mapNotNull {
                                val start = it["start"]?.toInt()
                                val end = it["end"]?.toInt()
                                if (start != null && end != null) Pair(start, end) else null
                            }
                        }
                        if (!TimeManager.hasFutureOrTodayDates(availableDates)) {
                            batch.delete(collectionRef.document(id))
                            return@mapNotNull null
                        }
                        Room(
                            id,
                            name,
                            description,
                            equipments,
                            maxParticipants,
                            availableDates,
                            availableHours
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                batch.commit()
                    .addOnSuccessListener {
                        onSuccess(rooms)
                    }.addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun parseRoomsFromJson(jsonString: String): List<Room> {
        val gson = Gson()

        data class RawRoom(
            val id: String,
            val name: String,
            val description: String,
            val equipments: List<String>,
            val maxParticipants: Int,
            val availableDates: List<String>,
            val availableHours: Map<String, List<Map<String, Int>>>
        )

        val rawRoomListType = object : TypeToken<List<RawRoom>>() {}.type
        val rawRooms: List<RawRoom> = gson.fromJson(jsonString, rawRoomListType)

        return rawRooms.map { rawRoom ->
            Room(
                id = rawRoom.id,
                name = rawRoom.name,
                description = rawRoom.description,
                equipments = rawRoom.equipments,
                maxParticipants = rawRoom.maxParticipants,
                availableDates = rawRoom.availableDates,
                availableHours = rawRoom.availableHours.mapValues { entry ->
                    entry.value.mapNotNull {
                        val start = it["start"]
                        val end = it["end"]
                        if (start != null && end != null) Pair(start, end) else null
                    }
                }
            )
        }
    }

    fun uploadInitialRoomsData(
        jsonString: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val rooms = parseRoomsFromJson(jsonString)
            saveRooms(rooms, onSuccess, onFailure)
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
