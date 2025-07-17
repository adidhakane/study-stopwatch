package com.example.simplestopwatch.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.simplestopwatch.R
import com.example.simplestopwatch.data.ActivityCategory

class CategoryAdapter(
    private var categories: List<ActivityCategory>,
    private val onCategorySelected: (ActivityCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = -1

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryCard: CardView = itemView.findViewById(R.id.categoryCard)
        val categoryIcon: TextView = itemView.findViewById(R.id.categoryIcon)
        val categoryName: TextView = itemView.findViewById(R.id.categoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        
        holder.categoryIcon.text = category.icon
        holder.categoryName.text = category.name
        
        // Set card background based on selection
        val isSelected = position == selectedPosition
        val backgroundColor = if (isSelected) {
            ContextCompat.getColor(holder.itemView.context, R.color.primary_color)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.dark_surface)
        }
        holder.categoryCard.setCardBackgroundColor(backgroundColor)
        
        // Set text color based on selection
        val textColor = if (isSelected) {
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
        }
        holder.categoryName.setTextColor(textColor)
        
        holder.categoryCard.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            
            onCategorySelected(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<ActivityCategory>) {
        categories = newCategories
        selectedPosition = -1
        notifyDataSetChanged()
    }

    fun selectCategory(categoryId: String) {
        val position = categories.indexOfFirst { it.id == categoryId }
        if (position != -1) {
            selectedPosition = position
            notifyDataSetChanged()
        }
    }
}
