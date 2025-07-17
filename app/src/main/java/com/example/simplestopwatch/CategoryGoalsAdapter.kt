package com.example.simplestopwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryGoalsAdapter(
    private val categoryGoals: MutableList<CategoryGoal>,
    private val onDeleteClick: (CategoryGoal) -> Unit
) : RecyclerView.Adapter<CategoryGoalsAdapter.CategoryGoalViewHolder>() {

    class CategoryGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryNameText: TextView = itemView.findViewById(R.id.categoryNameText)
        val categoryProgressBar: ProgressBar = itemView.findViewById(R.id.categoryProgressBar)
        val categoryPercentageText: TextView = itemView.findViewById(R.id.categoryPercentageText)
        val categoryProgressText: TextView = itemView.findViewById(R.id.categoryProgressText)
        val categoryGoalText: TextView = itemView.findViewById(R.id.categoryGoalText)
        val deleteCategoryGoalButton: ImageButton = itemView.findViewById(R.id.deleteCategoryGoalButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_goal, parent, false)
        return CategoryGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryGoalViewHolder, position: Int) {
        val categoryGoal = categoryGoals[position]
        
        holder.categoryNameText.text = categoryGoal.categoryName
        
        // Calculate progress percentage
        val progressPercentage = if (categoryGoal.goalMinutes > 0) {
            ((categoryGoal.currentProgress * 100) / categoryGoal.goalMinutes)
        } else 0
        
        holder.categoryProgressBar.progress = progressPercentage
        holder.categoryPercentageText.text = "${progressPercentage}%"
        
        // Format time display
        val achievedHours = categoryGoal.currentProgress / 60
        val achievedMins = categoryGoal.currentProgress % 60
        val goalHours = categoryGoal.goalMinutes / 60
        val goalMins = categoryGoal.goalMinutes % 60
        
        holder.categoryProgressText.text = "${achievedHours}h ${achievedMins}m today"
        holder.categoryGoalText.text = "${goalHours}h ${goalMins}m"
        
        // Set progress bar color based on achievement
        val progressColor = when {
            progressPercentage >= 100 -> R.color.success_color
            progressPercentage >= 75 -> R.color.primary_color
            progressPercentage >= 50 -> R.color.warning_color
            else -> R.color.error_color
        }
        holder.categoryProgressBar.progressTintList = 
            holder.itemView.context.getColorStateList(progressColor)
        holder.categoryPercentageText.setTextColor(
            holder.itemView.context.getColor(progressColor)
        )
        
        holder.deleteCategoryGoalButton.setOnClickListener {
            onDeleteClick(categoryGoal)
        }
    }

    override fun getItemCount(): Int = categoryGoals.size

    fun updateCategoryGoals(newGoals: List<CategoryGoal>) {
        categoryGoals.clear()
        categoryGoals.addAll(newGoals)
        notifyDataSetChanged()
    }

    fun removeCategoryGoal(categoryGoal: CategoryGoal) {
        val position = categoryGoals.indexOf(categoryGoal)
        if (position != -1) {
            categoryGoals.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
