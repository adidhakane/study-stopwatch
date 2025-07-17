package com.example.simplestopwatch.data

data class ActivityCategory(
    val id: String = "",
    val name: String = "",
    val type: ActivityType = ActivityType.STUDY,
    val color: String = "#2196F3", // Default blue color
    val icon: String = "📚", // Default icon
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActivityType {
    STUDY, TIMEPASS
}

// Predefined categories
object DefaultCategories {
    val studyCategories = listOf(
        ActivityCategory("dev", "Development", ActivityType.STUDY, "#4CAF50", "💻"),
        ActivityCategory("dsa", "DSA", ActivityType.STUDY, "#FF9800", "🧮"),
        ActivityCategory("theory", "Theory", ActivityType.STUDY, "#2196F3", "📖"),
        ActivityCategory("study_other", "Others", ActivityType.STUDY, "#9C27B0", "📚")
    )
    
    val timepassCategories = listOf(
        ActivityCategory("youtube", "YouTube", ActivityType.TIMEPASS, "#FF0000", "📺"),
        ActivityCategory("tv", "TV", ActivityType.TIMEPASS, "#FF5722", "📻"),
        ActivityCategory("timepass_other", "Others", ActivityType.TIMEPASS, "#795548", "🎮")
    )
}
