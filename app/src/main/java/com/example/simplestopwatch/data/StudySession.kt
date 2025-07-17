package com.example.simplestopwatch.data

data class StudySession(
    val id: String = "",
    val activityId: String = "",
    val activityName: String = "",
    val activityType: ActivityType = ActivityType.STUDY,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val duration: Long = 0L, // in milliseconds
    val date: String = "", // YYYY-MM-DD format
    val plannedStartTime: Long? = null, // for schedule tracking
    val actualStartTime: Long = 0L,
    val isOnTime: Boolean = true,
    val delayMinutes: Int = 0,
    val notes: String = "",
    val rating: Int = 0, // 1-5 productivity rating
    val createdAt: Long = System.currentTimeMillis()
)

data class DailyGoal(
    val date: String = "", // YYYY-MM-DD
    val studyGoalMinutes: Int = 0,
    val actualStudyMinutes: Int = 0,
    val timepassLimitMinutes: Int = 0,
    val actualTimepassMinutes: Int = 0,
    val isGoalAchieved: Boolean = false,
    val streak: Int = 0
)

data class ScheduleItem(
    val id: String = "",
    val title: String = "",
    val startTime: String = "", // HH:mm format
    val endTime: String = "", // HH:mm format
    val activityType: ActivityType = ActivityType.STUDY,
    val isFlexible: Boolean = true, // allows for delay tolerance
    val toleranceMinutes: Int = 30, // how much delay is acceptable
    val isActive: Boolean = true,
    val daysOfWeek: List<Int> = listOf(1,2,3,4,5,6,7), // 1=Monday, 7=Sunday
    val reminderMinutes: Int = 15 // remind before this many minutes
)
