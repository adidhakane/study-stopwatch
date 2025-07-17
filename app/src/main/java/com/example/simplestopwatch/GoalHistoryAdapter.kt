package com.example.simplestopwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class GoalHistoryAdapter(
    private val goalHistory: MutableList<GoalHistoryItem>
) : RecyclerView.Adapter<GoalHistoryAdapter.GoalHistoryViewHolder>() {

    class GoalHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val goalDateText: TextView = itemView.findViewById(R.id.goalDateText)
        val goalStatusText: TextView = itemView.findViewById(R.id.goalStatusText)
        val dailyGoalTargetText: TextView = itemView.findViewById(R.id.dailyGoalTargetText)
        val dailyGoalAchievedText: TextView = itemView.findViewById(R.id.dailyGoalAchievedText)
        val weeklyGoalLayout: LinearLayout = itemView.findViewById(R.id.weeklyGoalLayout)
        val weeklyGoalTargetText: TextView = itemView.findViewById(R.id.weeklyGoalTargetText)
        val weeklyGoalProgressText: TextView = itemView.findViewById(R.id.weeklyGoalProgressText)
        val achievementProgressBar: ProgressBar = itemView.findViewById(R.id.achievementProgressBar)
        val achievementPercentageText: TextView = itemView.findViewById(R.id.achievementPercentageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_history, parent, false)
        return GoalHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalHistoryViewHolder, position: Int) {
        val goalHistoryItem = goalHistory[position]
        val context = holder.itemView.context
        
        // Format and display date
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = dateFormat.parse(goalHistoryItem.date)
            holder.goalDateText.text = if (date != null) displayFormat.format(date) else goalHistoryItem.date
        } catch (e: Exception) {
            holder.goalDateText.text = goalHistoryItem.date
        }
        
        // Set status based on achievement
        val statusText: String
        val statusColor: Int
        val progressPercentage = goalHistoryItem.dailyProgress
        
        when {
            goalHistoryItem.dailyGoalAchieved -> {
                statusText = "✅ Achieved"
                statusColor = R.color.success_color
            }
            progressPercentage >= 80 -> {
                statusText = "📈 Close"
                statusColor = R.color.warning_color
            }
            else -> {
                statusText = "❌ Missed"
                statusColor = R.color.error_color
            }
        }
        
        holder.goalStatusText.text = statusText
        holder.goalStatusText.setTextColor(context.getColor(statusColor))
        
        // Format time display
        val goalHours = goalHistoryItem.goalMinutes / 60
        val goalMins = goalHistoryItem.goalMinutes % 60
        val studiedHours = goalHistoryItem.studyMinutes / 60
        val studiedMins = goalHistoryItem.studyMinutes % 60
        
        // Daily goal details
        holder.dailyGoalTargetText.text = "${goalHours}h ${goalMins}m"
        holder.dailyGoalAchievedText.text = "${studiedHours}h ${studiedMins}m"
        holder.dailyGoalAchievedText.setTextColor(
            context.getColor(if (goalHistoryItem.dailyGoalAchieved) R.color.success_color else R.color.error_color)
        )
        
        // Hide weekly goal layout for now (can be enabled when weekly goal tracking is implemented)
        holder.weeklyGoalLayout.visibility = View.GONE
        
        // Progress bar
        holder.achievementProgressBar.progress = minOf(progressPercentage, 100)
        holder.achievementProgressBar.progressTintList = context.getColorStateList(statusColor)
        holder.achievementPercentageText.text = "${progressPercentage}%"
        holder.achievementPercentageText.setTextColor(context.getColor(statusColor))
    }

    override fun getItemCount(): Int = goalHistory.size

    fun updateGoalHistory(newHistory: List<GoalHistoryItem>) {
        goalHistory.clear()
        goalHistory.addAll(newHistory.sortedByDescending { it.date })
        notifyDataSetChanged()
    }

    fun addGoalHistory(history: GoalHistoryItem) {
        goalHistory.add(0, history)
        notifyItemInserted(0)
    }
}
