package com.example.simplestopwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.simplestopwatch.data.*

class ScheduleAdapter(
    private var scheduleItems: List<ScheduleItem>,
    private val onItemClick: (ScheduleItem) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
        val activityTypeText: TextView = itemView.findViewById(R.id.activityTypeText)
        val daysText: TextView = itemView.findViewById(R.id.daysText)
        val flexibleBadge: TextView = itemView.findViewById(R.id.flexibleBadge)
        val editButton: Button = itemView.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = scheduleItems[position]
        
        holder.titleText.text = item.title
        holder.timeText.text = "${item.startTime} - ${item.endTime}"
        
        // Activity type with emoji
        val (emoji, typeName) = when (item.activityType) {
            ActivityType.STUDY -> "📚" to "Study"
            ActivityType.TIMEPASS -> "🎮" to "Timepass"
        }
        holder.activityTypeText.text = "$emoji $typeName"
        
        // Days of week
        val dayNames = item.daysOfWeek.map { dayNumber ->
            when (dayNumber) {
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                6 -> "Sat"
                7 -> "Sun"
                else -> ""
            }
        }.joinToString(", ")
        holder.daysText.text = dayNames
        
        // Flexible badge
        if (item.isFlexible) {
            holder.flexibleBadge.visibility = View.VISIBLE
            holder.flexibleBadge.text = "Flexible ±${item.toleranceMinutes}m"
        } else {
            holder.flexibleBadge.visibility = View.GONE
        }
        
        holder.editButton.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = scheduleItems.size

    fun updateItems(newItems: List<ScheduleItem>) {
        scheduleItems = newItems
        notifyDataSetChanged()
    }
}

class TodayScheduleAdapter(
    private var todayItems: List<TodayScheduleItem>,
    private val onCompleteClick: (TodayScheduleItem) -> Unit
) : RecyclerView.Adapter<TodayScheduleAdapter.TodayScheduleViewHolder>() {

    class TodayScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
        val statusBadge: TextView = itemView.findViewById(R.id.statusBadge)
        val activityTypeIcon: TextView = itemView.findViewById(R.id.activityTypeIcon)
        val completeButton: Button = itemView.findViewById(R.id.completeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodayScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_today_schedule, parent, false)
        return TodayScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodayScheduleViewHolder, position: Int) {
        val item = todayItems[position]
        val schedule = item.scheduleItem
        
        holder.titleText.text = schedule.title
        holder.timeText.text = "${schedule.startTime} - ${schedule.endTime}"
        
        // Activity type icon
        holder.activityTypeIcon.text = when (schedule.activityType) {
            ActivityType.STUDY -> "📚"
            ActivityType.TIMEPASS -> "🎮"
        }
        
        // Status badge with color
        val (statusText, statusColor) = when (item.status) {
            ScheduleStatus.UPCOMING -> "Upcoming" to R.color.text_secondary
            ScheduleStatus.REMINDER -> "Reminder" to R.color.warning_color
            ScheduleStatus.ACTIVE -> "Active Now" to R.color.success_color
            ScheduleStatus.OVERDUE -> "Overdue" to R.color.error_color
            ScheduleStatus.MISSED -> "Missed" to R.color.error_color
            ScheduleStatus.COMPLETED -> "Completed" to R.color.success_color
        }
        
        holder.statusBadge.text = statusText
        holder.statusBadge.setTextColor(holder.itemView.context.getColor(statusColor))
        
        // Complete button
        if (item.isCompleted) {
            holder.completeButton.text = "✓ Completed"
            holder.completeButton.isEnabled = false
            holder.completeButton.setBackgroundColor(holder.itemView.context.getColor(R.color.success_color))
        } else {
            holder.completeButton.text = "Mark Complete"
            holder.completeButton.isEnabled = true
            holder.completeButton.setBackgroundColor(holder.itemView.context.getColor(R.color.primary_color))
            
            holder.completeButton.setOnClickListener {
                onCompleteClick(item)
            }
        }
    }

    override fun getItemCount() = todayItems.size
}
