package com.example.chairbnb.Classes.Displaments

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.chairbnb.Classes.BookinRooms.RoomWithAvailableTime
import com.example.chairbnb.Objects.ObjectsHelper.TimeManager
import com.example.chairbnb.R

class RoomAdapter(
    private val rooms: List<RoomWithAvailableTime>,
    private val isForBooking: Boolean = true,
    private val onBookingCancelConfirmed: (RoomWithAvailableTime) -> Unit = {},
    private val onRoomSelected: (RoomWithAvailableTime) -> Unit = {}
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.room_details, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(rooms[position])
    }

    override fun getItemCount(): Int = rooms.size

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val roomName: TextView = itemView.findViewById(R.id.roomName)
        private val roomDescription: TextView = itemView.findViewById(R.id.roomDescription)
        private val roomAvailableTime: TextView = itemView.findViewById(R.id.roomAvailableTime)
        private val maxParticipants: TextView = itemView.findViewById(R.id.maxParticipantsText)
        private val equipmentsTextView: TextView = itemView.findViewById(R.id.equipmentsTextView)

        fun bind(roomWithTime: RoomWithAvailableTime) {
            val room = roomWithTime.room
            roomName.text = room.name
            roomDescription.text = room.description
            val hourglassDrawable =
                ContextCompat.getDrawable(itemView.context, R.drawable.hourglass)
            val groupDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.group)
            roomAvailableTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                hourglassDrawable,
                null,
                null,
                null
            )
            displayAvailability(roomWithTime)
            maxParticipants.text = room.maxParticipants.toString()
            maxParticipants.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null, groupDrawable, null
            )
            equipmentsTextView.text = room.equipments.joinToString(",")
            itemView.setOnClickListener {
                if (isForBooking) onRoomSelected(roomWithTime)
                else showCancelDialog(roomWithTime)
            }
        }

        private fun buildAvailabilityText(
            startHour: Int,
            startMinute: Int,
            endHour: Int,
            endMinute: Int,
            dateStr: String
        ): String {
            val timeRange = "${TimeManager.formatTime(startHour, startMinute)}–${
                TimeManager.formatTime(
                    endHour,
                    endMinute
                )
            }"
            return if (dateStr.isNotEmpty()) {
                "Available from $timeRange on $dateStr"
            } else {
                "Available from $timeRange"
            }
        }

        private fun displayAvailability(roomWithTime: RoomWithAvailableTime) {
            val start = roomWithTime.suggestedStartTime ?: "?"
            val startHour = TimeManager.getHour(start)
            val startMinute = TimeManager.getMinutes(start)
            if (startHour != null) {
                val (endHour, endMinute) = TimeManager.calculateEndTime(
                    startHour,
                    startMinute, roomWithTime.availableHoursFromNow
                )
                val dateStr = roomWithTime.suggestedDate ?: ""
                roomAvailableTime.text = buildAvailabilityText(
                    startHour,
                    startMinute, endHour, endMinute, dateStr
                )
            } else {
                roomAvailableTime.text = "Availability unknown"
            }
        }

        private fun showCancelDialog(roomWithTime: RoomWithAvailableTime) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Confirm cancellation")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes") { dialog, _ ->
                    onBookingCancelConfirmed(roomWithTime)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

    }
}
