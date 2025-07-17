package com.example.simplestopwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DailyStatsAdapter(private val dailyStats: List<DailyStatistic>) :
    RecyclerView.Adapter<DailyStatsAdapter.DailyStatsViewHolder>() {

    class DailyStatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val studyTimeText: TextView = itemView.findViewById(R.id.studyTimeText)
        val timepassTimeText: TextView = itemView.findViewById(R.id.timepassTimeText)
        val goalProgressBar: ProgressBar = itemView.findViewById(R.id.goalProgressBar)
        val goalPercentageText: TextView = itemView.findViewById(R.id.goalPercentageText)
        val categoryBreakdownText: TextView = itemView.findViewById(R.id.categoryBreakdownText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyStatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_stats, parent, false)
        return DailyStatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyStatsViewHolder, position: Int) {
        val stats = dailyStats[position]
        
        // Format date
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, EEE", Locale.getDefault())
        val date = inputFormat.parse(stats.date)
        holder.dateText.text = if (date != null) outputFormat.format(date) else stats.date
        
        // Display times
        holder.studyTimeText.text = formatTime(stats.studyMinutes)
        holder.timepassTimeText.text = formatTime(stats.timepassMinutes)
        
        // Calculate and display goal progress
        val goalProgress = if (stats.studyGoalMinutes > 0) {
            (stats.studyMinutes.toFloat() / stats.studyGoalMinutes * 100).toInt()
        } else 0
        
        holder.goalProgressBar.progress = goalProgress.coerceAtMost(100)
        holder.goalPercentageText.text = "$goalProgress%"
        
        // Display category breakdown
        val breakdown = stats.categories.entries
            .filter { it.value > 0 }
            .joinToString(", ") { "${it.key}: ${formatTime(it.value)}" }
        
        holder.categoryBreakdownText.text = if (breakdown.isNotEmpty()) breakdown else "No activities"
    }

    override fun getItemCount() = dailyStats.size

    private fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }
}

class WeeklyStatsAdapter(private val weeklyStats: List<WeeklyStatistic>) :
    RecyclerView.Adapter<WeeklyStatsAdapter.WeeklyStatsViewHolder>() {

    class WeeklyStatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val weekText: TextView = itemView.findViewById(R.id.weekText)
        val totalStudyText: TextView = itemView.findViewById(R.id.totalStudyText)
        val avgDailyText: TextView = itemView.findViewById(R.id.avgDailyText)
        val studyDaysText: TextView = itemView.findViewById(R.id.studyDaysText)
        val studyProgressBar: ProgressBar = itemView.findViewById(R.id.studyProgressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyStatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_stats, parent, false)
        return WeeklyStatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeeklyStatsViewHolder, position: Int) {
        val stats = weeklyStats[position]
        
        // Format week
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val startDate = inputFormat.parse(stats.weekStart)
        val calendar = Calendar.getInstance()
        if (startDate != null) {
            calendar.time = startDate
            calendar.add(Calendar.DAY_OF_MONTH, 6)
            val endDate = calendar.time
            holder.weekText.text = "${outputFormat.format(startDate)} - ${outputFormat.format(endDate)}"
        } else {
            holder.weekText.text = stats.weekStart
        }
        
        // Display statistics
        holder.totalStudyText.text = formatTime(stats.studyMinutes)
        holder.avgDailyText.text = formatTime(stats.avgDailyStudy)
        holder.studyDaysText.text = "${stats.studyDays}/7 days"
        
        // Progress bar (assuming 7 hours per week as target)
        val weeklyGoal = 7 * 60 // 7 hours in minutes
        val progress = (stats.studyMinutes.toFloat() / weeklyGoal * 100).toInt()
        holder.studyProgressBar.progress = progress.coerceAtMost(100)
    }

    override fun getItemCount() = weeklyStats.size

    private fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }
}
