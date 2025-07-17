package com.example.simplestopwatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.example.simplestopwatch.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class StatisticsFragment : Fragment() {
    
    private lateinit var database: DatabaseReference
    private lateinit var dailyStatsRecyclerView: RecyclerView
    private lateinit var weeklyStatsRecyclerView: RecyclerView
    private lateinit var totalStudyTimeText: TextView
    private lateinit var totalTimepassTimeText: TextView
    private lateinit var todayStudyTimeText: TextView
    private lateinit var todayTimepassTimeText: TextView
    private lateinit var dailyStatsAdapter: DailyStatsAdapter
    private lateinit var weeklyStatsAdapter: WeeklyStatsAdapter
    
    private val dailyStats = mutableListOf<DailyStatistic>()
    private val weeklyStats = mutableListOf<WeeklyStatistic>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerViews()
        initializeDatabase()
        loadStatistics()
    }
    
    private fun initializeViews(view: View) {
        dailyStatsRecyclerView = view.findViewById(R.id.dailyStatsRecyclerView)
        weeklyStatsRecyclerView = view.findViewById(R.id.weeklyStatsRecyclerView)
        totalStudyTimeText = view.findViewById(R.id.totalStudyTimeText)
        totalTimepassTimeText = view.findViewById(R.id.totalTimepassTimeText)
        todayStudyTimeText = view.findViewById(R.id.todayStudyTimeText)
        todayTimepassTimeText = view.findViewById(R.id.todayTimepassTimeText)
    }
    
    private fun setupRecyclerViews() {
        dailyStatsAdapter = DailyStatsAdapter(dailyStats)
        dailyStatsRecyclerView.apply {
            adapter = dailyStatsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        weeklyStatsAdapter = WeeklyStatsAdapter(weeklyStats)
        weeklyStatsRecyclerView.apply {
            adapter = weeklyStatsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }
    
    private fun initializeDatabase() {
        database = FirebaseDatabase.getInstance().reference
    }
    
    private fun loadStatistics() {
        loadTodayStats()
        loadTotalStats()
        loadDailyStats()
        loadWeeklyStats()
    }
    
    private fun loadTodayStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        database.child("daily_statistics").child(today)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val todayStats = snapshot.getValue(DailyGoal::class.java)
                    if (todayStats != null) {
                        todayStudyTimeText.text = formatTime(todayStats.actualStudyMinutes)
                        todayTimepassTimeText.text = formatTime(todayStats.actualTimepassMinutes)
                    } else {
                        todayStudyTimeText.text = "00:00"
                        todayTimepassTimeText.text = "00:00"
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun loadTotalStats() {
        database.child("study_sessions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalStudyMinutes = 0L
                    var totalTimepassMinutes = 0L
                    
                    for (sessionSnapshot in snapshot.children) {
                        val session = sessionSnapshot.getValue(StudySession::class.java)
                        session?.let {
                            val minutes = it.duration / (1000 * 60)
                            if (it.activityType == ActivityType.STUDY) {
                                totalStudyMinutes += minutes
                            } else {
                                totalTimepassMinutes += minutes
                            }
                        }
                    }
                    
                    totalStudyTimeText.text = formatTime(totalStudyMinutes.toInt())
                    totalTimepassTimeText.text = formatTime(totalTimepassMinutes.toInt())
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun loadDailyStats() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30) // Last 30 days
        
        database.child("daily_statistics")
            .orderByKey()
            .startAt(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time))
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dailyStats.clear()
                    
                    for (daySnapshot in snapshot.children) {
                        val date = daySnapshot.key ?: continue
                        val stats = daySnapshot.getValue(DailyGoal::class.java)
                        
                        if (stats != null) {
                            dailyStats.add(
                                DailyStatistic(
                                    date = date,
                                    studyMinutes = stats.actualStudyMinutes,
                                    timepassMinutes = stats.actualTimepassMinutes,
                                    studyGoalMinutes = stats.studyGoalMinutes,
                                    categories = getCategoryBreakdown(stats)
                                )
                            )
                        }
                    }
                    
                    dailyStats.sortByDescending { it.date }
                    dailyStatsAdapter.notifyDataSetChanged()
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun loadWeeklyStats() {
        // Calculate weekly statistics from daily data
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -12) // Last 12 weeks
        
        database.child("daily_statistics")
            .orderByKey()
            .startAt(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time))
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val weeklyData = mutableMapOf<String, WeeklyStatistic>()
                    
                    for (daySnapshot in snapshot.children) {
                        val date = daySnapshot.key ?: continue
                        val stats = daySnapshot.getValue(DailyGoal::class.java) ?: continue
                        
                        val weekKey = getWeekKey(date)
                        val existing = weeklyData[weekKey] ?: WeeklyStatistic(
                            weekStart = weekKey,
                            studyMinutes = 0,
                            timepassMinutes = 0,
                            avgDailyStudy = 0,
                            studyDays = 0
                        )
                        
                        weeklyData[weekKey] = existing.copy(
                            studyMinutes = existing.studyMinutes + stats.actualStudyMinutes,
                            timepassMinutes = existing.timepassMinutes + stats.actualTimepassMinutes,
                            studyDays = if (stats.actualStudyMinutes > 0) existing.studyDays + 1 else existing.studyDays
                        )
                    }
                    
                    weeklyStats.clear()
                    weeklyStats.addAll(weeklyData.values.sortedByDescending { it.weekStart })
                    
                    // Calculate averages
                    weeklyStats.forEach { week ->
                        week.avgDailyStudy = if (week.studyDays > 0) week.studyMinutes / week.studyDays else 0
                    }
                    
                    weeklyStatsAdapter.notifyDataSetChanged()
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun getCategoryBreakdown(stats: DailyGoal): Map<String, Int> {
        // This would ideally come from session data grouped by category
        // For now, return a simple breakdown
        return mapOf(
            "Development" to (stats.actualStudyMinutes * 0.4).toInt(),
            "DSA" to (stats.actualStudyMinutes * 0.3).toInt(),
            "Theory" to (stats.actualStudyMinutes * 0.2).toInt(),
            "Others" to (stats.actualStudyMinutes * 0.1).toInt()
        )
    }
    
    private fun getWeekKey(dateString: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString) ?: return dateString
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
    
    private fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }
}

// Data classes for statistics
data class DailyStatistic(
    val date: String,
    val studyMinutes: Int,
    val timepassMinutes: Int,
    val studyGoalMinutes: Int,
    val categories: Map<String, Int>
)

data class WeeklyStatistic(
    val weekStart: String,
    var studyMinutes: Int,
    var timepassMinutes: Int,
    var avgDailyStudy: Int,
    var studyDays: Int
)
