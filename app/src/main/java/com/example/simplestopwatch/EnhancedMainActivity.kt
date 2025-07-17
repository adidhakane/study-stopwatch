package com.example.simplestopwatch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.FirebaseDatabase
import com.example.simplestopwatch.adapters.CategoryAdapter
import com.example.simplestopwatch.data.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EnhancedMainActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var startButton: FloatingActionButton
    private lateinit var pauseButton: FloatingActionButton
    private lateinit var resetButton: FloatingActionButton
    private lateinit var activityTypeToggle: LinearLayout
    private lateinit var studyButton: Button
    private lateinit var timepassButton: Button
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var selectedActivityCard: CardView
    private lateinit var selectedActivityIcon: TextView
    private lateinit var selectedActivityName: TextView
    private lateinit var selectedActivityType: TextView
    private lateinit var changeActivityButton: Button
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var addCategoryButton: Button
    private lateinit var activitySelectionCard: CardView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    // Fragment management
    private var timerContentView: View? = null
    private var currentFragment: Fragment? = null

    // Adapters
    private lateinit var categoryAdapter: CategoryAdapter

    // Timer Logic
    private var handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var pausedTime = 0L
    private var isRunning = false

    // Activity Selection
    private var selectedCategory: ActivityCategory? = null
    private var currentActivityType = ActivityType.STUDY

    private val runnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis() - startTime
            updateTimerText(currentTime)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_enhanced)

        initializeViews()
        setupCategoryRecyclerView()
        setupEventListeners()
        loadDefaultCategories()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        resetButton = findViewById(R.id.resetButton)
        activityTypeToggle = findViewById(R.id.activityTypeToggle)
        studyButton = findViewById(R.id.studyButton)
        timepassButton = findViewById(R.id.timepassButton)
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView)
        selectedActivityCard = findViewById(R.id.selectedActivityCard)
        selectedActivityIcon = findViewById(R.id.selectedActivityIcon)
        selectedActivityName = findViewById(R.id.selectedActivityName)
        selectedActivityType = findViewById(R.id.selectedActivityType)
        changeActivityButton = findViewById(R.id.changeActivityButton)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        addCategoryButton = findViewById(R.id.addCategoryButton)
        activitySelectionCard = findViewById(R.id.activitySelectionCard)
    }

    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryAdapter(emptyList()) { category ->
            selectActivity(category)
        }
        categoriesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        categoriesRecyclerView.adapter = categoryAdapter
    }

    private fun setupEventListeners() {
        startButton.setOnClickListener { startTimer() }
        pauseButton.setOnClickListener { pauseTimer() }
        resetButton.setOnClickListener { stopAndSaveSession() }

        studyButton.setOnClickListener {
            selectActivityType(ActivityType.STUDY)
        }
        
        timepassButton.setOnClickListener {
            selectActivityType(ActivityType.TIMEPASS)
        }

        changeActivityButton.setOnClickListener {
            showActivitySelection()
        }

        addCategoryButton.setOnClickListener {
            // TODO: Implement add custom category dialog
            Toast.makeText(this, "Add custom category feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_timer -> {
                    showTimerView()
                    true
                }
                R.id.nav_statistics -> {
                    showStatisticsFragment()
                    true
                }
                R.id.nav_schedule -> {
                    showScheduleFragment()
                    true
                }
                R.id.nav_goals -> {
                    showGoalsFragment()
                    true
                }
                R.id.nav_settings -> {
                    showSettingsFragment()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadDefaultCategories() {
        loadCategoriesForType(ActivityType.STUDY)
        selectActivityType(ActivityType.STUDY)
    }

    private fun selectActivityType(type: ActivityType) {
        currentActivityType = type
        when (type) {
            ActivityType.STUDY -> {
                studyButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
                timepassButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_surface))
                loadCategoriesForType(ActivityType.STUDY)
            }
            ActivityType.TIMEPASS -> {
                timepassButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
                studyButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_surface))
                loadCategoriesForType(ActivityType.TIMEPASS)
            }
        }
    }

    private fun loadCategoriesForType(type: ActivityType) {
        val categories = when (type) {
            ActivityType.STUDY -> DefaultCategories.studyCategories
            ActivityType.TIMEPASS -> DefaultCategories.timepassCategories
        }
        categoryAdapter.updateCategories(categories)
        selectedCategory = null
        hideSelectedActivity()
    }

    private fun selectActivity(category: ActivityCategory) {
        selectedCategory = category
        showSelectedActivity(category)
    }

    private fun showSelectedActivity(category: ActivityCategory) {
        selectedActivityIcon.text = category.icon
        selectedActivityName.text = category.name
        selectedActivityType.text = if (category.type == ActivityType.STUDY) "Study" else "Timepass"
        
        selectedActivityCard.visibility = View.VISIBLE
        activitySelectionCard.visibility = View.GONE
        
        // Enable start button only when activity is selected
        startButton.isEnabled = true
    }

    private fun hideSelectedActivity() {
        selectedActivityCard.visibility = View.GONE
        activitySelectionCard.visibility = View.VISIBLE
        startButton.isEnabled = false
    }

    private fun showActivitySelection() {
        hideSelectedActivity()
        selectedCategory = null
    }

    private fun startTimer() {
        if (selectedCategory == null) {
            Toast.makeText(this, "Please select an activity first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isRunning) {
            startTime = System.currentTimeMillis() - pausedTime
            handler.postDelayed(runnable, 0)
            isRunning = true
            startButton.isEnabled = false
            pauseButton.isEnabled = true
            resetButton.isEnabled = false
            
            // Hide activity selection during timer
            selectedActivityCard.visibility = View.VISIBLE
            activitySelectionCard.visibility = View.GONE
        }
    }

    private fun pauseTimer() {
        if (isRunning) {
            handler.removeCallbacks(runnable)
            pausedTime = System.currentTimeMillis() - startTime
            isRunning = false
            startButton.isEnabled = true
            pauseButton.isEnabled = false
            resetButton.isEnabled = true
        }
    }

    private fun stopAndSaveSession() {
        if (selectedCategory == null) return

        handler.removeCallbacks(runnable)
        
        val sessionDuration = if (pausedTime > 0) pausedTime else System.currentTimeMillis() - startTime
        
        // Create session object
        val session = StudySession(
            id = UUID.randomUUID().toString(),
            activityId = selectedCategory!!.id,
            activityName = selectedCategory!!.name,
            activityType = selectedCategory!!.type,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            duration = sessionDuration,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            actualStartTime = startTime,
            isOnTime = true, // TODO: Calculate based on schedule
            delayMinutes = 0, // TODO: Calculate based on schedule
            notes = "",
            rating = 0
        )

        saveSessionToFirebase(session) {
            resetTimer()
            showActivitySelection()
        }
    }

    private fun resetTimer() {
        handler.removeCallbacks(runnable)
        timerTextView.text = "00:00:00"
        pausedTime = 0
        isRunning = false
        startButton.isEnabled = selectedCategory != null
        pauseButton.isEnabled = false
        resetButton.isEnabled = false
    }

    private fun updateTimerText(millis: Long) {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun saveSessionToFirebase(session: StudySession, onComplete: () -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val sessionsRef = database.getReference("study_sessions")
        
        sessionsRef.child(session.id).setValue(session)
            .addOnSuccessListener {
                Toast.makeText(this, "Session saved successfully!", Toast.LENGTH_SHORT).show()
                updateDailyStats(session)
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save session", Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }

    private fun updateDailyStats(session: StudySession) {
        val database = FirebaseDatabase.getInstance()
        val dailyStatsRef = database.getReference("daily_stats").child(session.date)
        
        dailyStatsRef.get().addOnSuccessListener { snapshot ->
            val existingStats = snapshot.getValue(DailyGoal::class.java) ?: DailyGoal(date = session.date)
            
            val updatedStats = when (session.activityType) {
                ActivityType.STUDY -> existingStats.copy(
                    actualStudyMinutes = existingStats.actualStudyMinutes + (session.duration / 60000).toInt()
                )
                ActivityType.TIMEPASS -> existingStats.copy(
                    actualTimepassMinutes = existingStats.actualTimepassMinutes + (session.duration / 60000).toInt()
                )
            }
            
            dailyStatsRef.setValue(updatedStats)
        }
    }
    
    private fun showTimerView() {
        // Clear any fragments from the container
        currentFragment?.let {
            supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
        currentFragment = null
        
        // Show timer content and hide fragment container
        findViewById<View>(R.id.timerContent)?.visibility = View.VISIBLE
        findViewById<View>(R.id.fragmentContainer)?.visibility = View.GONE
        
        // Update toolbar title
        toolbar.title = "Activity Timer"
    }
    
    private fun showStatisticsFragment() {
        // Hide timer content and show fragment container
        findViewById<View>(R.id.timerContent)?.visibility = View.GONE
        findViewById<View>(R.id.fragmentContainer)?.visibility = View.VISIBLE
        
        // Replace any existing fragment with statistics fragment
        val statsFragment = StatisticsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, statsFragment, "STATISTICS")
            .commit()
        
        currentFragment = statsFragment
        
        // Update toolbar title
        toolbar.title = "Statistics"
    }
    
    private fun showScheduleFragment() {
        // Hide timer content and show fragment container
        findViewById<View>(R.id.timerContent)?.visibility = View.GONE
        findViewById<View>(R.id.fragmentContainer)?.visibility = View.VISIBLE
        
        // Replace any existing fragment with schedule fragment
        val scheduleFragment = ScheduleFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, scheduleFragment, "SCHEDULE")
            .commit()
        
        currentFragment = scheduleFragment
        
        // Update toolbar title
        toolbar.title = "Schedule Management"
    }
    
    private fun showGoalsFragment() {
        // Hide timer content and show fragment container
        findViewById<View>(R.id.timerContent)?.visibility = View.GONE
        findViewById<View>(R.id.fragmentContainer)?.visibility = View.VISIBLE
        
        // Replace any existing fragment with goals fragment
        val goalsFragment = GoalsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, goalsFragment, "GOALS")
            .commit()
        
        currentFragment = goalsFragment
        
        // Update toolbar title
        toolbar.title = "Goals & Progress"
    }
    
    private fun showSettingsFragment() {
        // Hide timer content and show fragment container
        findViewById<View>(R.id.timerContent)?.visibility = View.GONE
        findViewById<View>(R.id.fragmentContainer)?.visibility = View.VISIBLE
        
        // Replace any existing fragment with settings fragment
        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment, "SETTINGS")
            .commit()
        
        currentFragment = settingsFragment
        
        // Update toolbar title
        toolbar.title = "Settings"
    }
}
