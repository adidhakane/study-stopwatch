package com.example.simplestopwatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.example.simplestopwatch.data.*
import java.text.SimpleDateFormat
import java.util.*

class GoalsFragment : Fragment() {
    
    private lateinit var database: DatabaseReference
    private lateinit var dailyGoalProgressBar: ProgressBar
    private lateinit var weeklyGoalProgressBar: ProgressBar
    private lateinit var dailyGoalText: TextView
    private lateinit var weeklyGoalText: TextView
    private lateinit var dailyProgressText: TextView
    private lateinit var weeklyProgressText: TextView
    private lateinit var categoryGoalsRecyclerView: RecyclerView
    private lateinit var goalHistoryRecyclerView: RecyclerView
    private lateinit var setDailyGoalButton: Button
    private lateinit var setWeeklyGoalButton: Button
    private lateinit var addCategoryGoalButton: FloatingActionButton
    private lateinit var categoryGoalsAdapter: CategoryGoalsAdapter
    private lateinit var goalHistoryAdapter: GoalHistoryAdapter
    
    private val categoryGoals = mutableListOf<CategoryGoal>()
    private val goalHistory = mutableListOf<GoalHistoryItem>()
    private var currentDailyGoal = 0
    private var currentWeeklyGoal = 0
    private var todayStudyMinutes = 0
    private var weekStudyMinutes = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goals, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerViews()
        initializeDatabase()
        loadGoalData()
        loadProgressData()
        loadGoalHistory()
    }
    
    private fun initializeViews(view: View) {
        dailyGoalProgressBar = view.findViewById(R.id.dailyGoalProgressBar)
        weeklyGoalProgressBar = view.findViewById(R.id.weeklyGoalProgressBar)
        dailyGoalText = view.findViewById(R.id.dailyGoalText)
        weeklyGoalText = view.findViewById(R.id.weeklyGoalText)
        dailyProgressText = view.findViewById(R.id.dailyProgressText)
        weeklyProgressText = view.findViewById(R.id.weeklyProgressText)
        categoryGoalsRecyclerView = view.findViewById(R.id.categoryGoalsRecyclerView)
        goalHistoryRecyclerView = view.findViewById(R.id.goalHistoryRecyclerView)
        setDailyGoalButton = view.findViewById(R.id.setDailyGoalButton)
        setWeeklyGoalButton = view.findViewById(R.id.setWeeklyGoalButton)
        addCategoryGoalButton = view.findViewById(R.id.addCategoryGoalButton)
        
        setDailyGoalButton.setOnClickListener { showSetDailyGoalDialog() }
        setWeeklyGoalButton.setOnClickListener { showSetWeeklyGoalDialog() }
        addCategoryGoalButton.setOnClickListener { showAddCategoryGoalDialog() }
    }
    
    private fun setupRecyclerViews() {
        categoryGoalsAdapter = CategoryGoalsAdapter(categoryGoals) { goal ->
            showEditCategoryGoalDialog(goal)
        }
        
        categoryGoalsRecyclerView.apply {
            adapter = categoryGoalsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        goalHistoryAdapter = GoalHistoryAdapter(goalHistory)
        
        goalHistoryRecyclerView.apply {
            adapter = goalHistoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun initializeDatabase() {
        database = FirebaseDatabase.getInstance().reference
    }
    
    private fun loadGoalData() {
        // Load daily and weekly goals
        database.child("user_goals")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentDailyGoal = snapshot.child("daily_goal_minutes").getValue(Int::class.java) ?: 240 // Default 4 hours
                    currentWeeklyGoal = snapshot.child("weekly_goal_minutes").getValue(Int::class.java) ?: 1680 // Default 28 hours
                    
                    dailyGoalText.text = "${currentDailyGoal / 60}h ${currentDailyGoal % 60}m"
                    weeklyGoalText.text = "${currentWeeklyGoal / 60}h ${currentWeeklyGoal % 60}m"
                    
                    updateProgressBars()
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
        
        // Load category goals
        database.child("category_goals")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryGoals.clear()
                    
                    for (goalSnapshot in snapshot.children) {
                        val goal = goalSnapshot.getValue(CategoryGoal::class.java)
                        goal?.let { categoryGoals.add(it) }
                    }
                    
                    categoryGoalsAdapter.notifyDataSetChanged()
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    private fun loadProgressData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Load today's progress
        database.child("daily_statistics").child(today)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dailyStats = snapshot.getValue(DailyGoal::class.java)
                    todayStudyMinutes = dailyStats?.actualStudyMinutes ?: 0
                    
                    dailyProgressText.text = "${todayStudyMinutes / 60}h ${todayStudyMinutes % 60}m"
                    updateProgressBars()
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
        
        // Load this week's progress
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        database.child("daily_statistics")
            .orderByKey()
            .startAt(weekStart)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    weekStudyMinutes = 0
                    
                    for (daySnapshot in snapshot.children) {
                        val dailyStats = daySnapshot.getValue(DailyGoal::class.java)
                        weekStudyMinutes += dailyStats?.actualStudyMinutes ?: 0
                    }
                    
                    weeklyProgressText.text = "${weekStudyMinutes / 60}h ${weekStudyMinutes % 60}m"
                    updateProgressBars()
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    private fun loadGoalHistory() {
        database.child("goal_history")
            .orderByChild("date")
            .limitToLast(30)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    goalHistory.clear()
                    
                    for (historySnapshot in snapshot.children) {
                        val historyItem = historySnapshot.getValue(GoalHistoryItem::class.java)
                        historyItem?.let { goalHistory.add(it) }
                    }
                    
                    goalHistory.sortByDescending { it.date }
                    goalHistoryAdapter.notifyDataSetChanged()
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    private fun updateProgressBars() {
        // Daily progress
        val dailyProgress = if (currentDailyGoal > 0) {
            (todayStudyMinutes.toFloat() / currentDailyGoal * 100).toInt()
        } else 0
        
        dailyGoalProgressBar.progress = dailyProgress.coerceAtMost(100)
        
        // Weekly progress
        val weeklyProgress = if (currentWeeklyGoal > 0) {
            (weekStudyMinutes.toFloat() / currentWeeklyGoal * 100).toInt()
        } else 0
        
        weeklyGoalProgressBar.progress = weeklyProgress.coerceAtMost(100)
        
        // Update goal achievement status
        updateGoalAchievementStatus(dailyProgress, weeklyProgress)
    }
    
    private fun updateGoalAchievementStatus(dailyProgress: Int, weeklyProgress: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val goalStatus = GoalHistoryItem(
            id = today,
            date = today,
            dailyGoalAchieved = dailyProgress >= 100,
            weeklyGoalAchieved = weeklyProgress >= 100,
            dailyProgress = dailyProgress,
            weeklyProgress = weeklyProgress,
            studyMinutes = todayStudyMinutes,
            goalMinutes = currentDailyGoal
        )
        
        database.child("goal_history").child(today).setValue(goalStatus)
    }
    
    private fun showSetDailyGoalDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_goal, null)
        val hoursInput = dialogView.findViewById<EditText>(R.id.hoursInput)
        val minutesInput = dialogView.findViewById<EditText>(R.id.minutesInput)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        
        dialogTitle.text = "Set Daily Study Goal"
        hoursInput.setText((currentDailyGoal / 60).toString())
        minutesInput.setText((currentDailyGoal % 60).toString())
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Daily Study Goal")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val hours = hoursInput.text.toString().toIntOrNull() ?: 4
                val minutes = minutesInput.text.toString().toIntOrNull() ?: 0
                val totalMinutes = hours * 60 + minutes
                
                database.child("user_goals").child("daily_goal_minutes").setValue(totalMinutes)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Daily goal set successfully!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSetWeeklyGoalDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_goal, null)
        val hoursInput = dialogView.findViewById<EditText>(R.id.hoursInput)
        val minutesInput = dialogView.findViewById<EditText>(R.id.minutesInput)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        
        dialogTitle.text = "Set Weekly Study Goal"
        hoursInput.setText((currentWeeklyGoal / 60).toString())
        minutesInput.setText((currentWeeklyGoal % 60).toString())
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Weekly Study Goal")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val hours = hoursInput.text.toString().toIntOrNull() ?: 28
                val minutes = minutesInput.text.toString().toIntOrNull() ?: 0
                val totalMinutes = hours * 60 + minutes
                
                database.child("user_goals").child("weekly_goal_minutes").setValue(totalMinutes)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Weekly goal set successfully!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAddCategoryGoalDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_category_goal, null)
        
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        val hoursInput = dialogView.findViewById<EditText>(R.id.categoryHoursInput)
        val minutesInput = dialogView.findViewById<EditText>(R.id.categoryMinutesInput)
        
        // Setup category spinner
        val categories = DefaultCategories.studyCategories.map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Category Goal")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val categoryName = categories[categorySpinner.selectedItemPosition]
                val hours = hoursInput.text.toString().toIntOrNull() ?: 2
                val mins = minutesInput.text.toString().toIntOrNull() ?: 0
                val goalMinutes = hours * 60 + mins
                
                val categoryGoal = CategoryGoal(
                    id = database.child("category_goals").push().key ?: "",
                    categoryName = categoryName,
                    goalMinutes = goalMinutes,
                    period = "Daily",
                    currentProgress = 0,
                    isActive = true
                )
                
                database.child("category_goals").child(categoryGoal.id).setValue(categoryGoal)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Category goal added successfully!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditCategoryGoalDialog(goal: CategoryGoal) {
        Toast.makeText(context, "Edit category goal feature coming soon!", Toast.LENGTH_SHORT).show()
    }
}

// Data classes for goals
data class CategoryGoal(
    val id: String = "",
    val categoryName: String = "",
    val goalMinutes: Int = 0,
    val period: String = "Daily", // Daily, Weekly, Monthly
    val currentProgress: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class GoalHistoryItem(
    val id: String = "",
    val date: String = "",
    val dailyGoalAchieved: Boolean = false,
    val weeklyGoalAchieved: Boolean = false,
    val dailyProgress: Int = 0,
    val weeklyProgress: Int = 0,
    val studyMinutes: Int = 0,
    val goalMinutes: Int = 0
)
