package com.example.simplestopwatch

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.*

class SettingsFragment : Fragment() {
    
    private lateinit var database: DatabaseReference
    private lateinit var sharedPrefs: SharedPreferences
    
    // UI Components
    private lateinit var notificationsSwitch: Switch
    private lateinit var pomodoroSwitch: Switch
    private lateinit var autoBackupSwitch: Switch
    private lateinit var soundEffectsSwitch: Switch
    private lateinit var darkModeSwitch: Switch
    private lateinit var pomodoroWorkTimeSeekBar: SeekBar
    private lateinit var pomodoroBreakTimeSeekBar: SeekBar
    private lateinit var pomodoroWorkTimeText: TextView
    private lateinit var pomodoroBreakTimeText: TextView
    private lateinit var notificationTimeSpinner: Spinner
    private lateinit var exportDataButton: Button
    private lateinit var clearDataButton: Button
    private lateinit var resetSettingsButton: Button
    private lateinit var aboutButton: Button
    
    // Settings values
    private var notificationsEnabled = true
    private var pomodoroEnabled = false
    private var autoBackupEnabled = true
    private var soundEffectsEnabled = true
    private var darkModeEnabled = true
    private var pomodoroWorkMinutes = 25
    private var pomodoroBreakMinutes = 5
    private var notificationReminderMinutes = 30
    
    companion object {
        const val PREFS_NAME = "StudyStopwatchPrefs"
        const val PREF_NOTIFICATIONS = "notifications_enabled"
        const val PREF_POMODORO = "pomodoro_enabled"
        const val PREF_AUTO_BACKUP = "auto_backup_enabled"
        const val PREF_SOUND_EFFECTS = "sound_effects_enabled"
        const val PREF_DARK_MODE = "dark_mode_enabled"
        const val PREF_POMODORO_WORK = "pomodoro_work_minutes"
        const val PREF_POMODORO_BREAK = "pomodoro_break_minutes"
        const val PREF_NOTIFICATION_REMINDER = "notification_reminder_minutes"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeComponents(view)
        loadSettings()
        setupListeners()
    }
    
    private fun initializeComponents(view: View) {
        database = FirebaseDatabase.getInstance().reference
        sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Find UI components
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch)
        pomodoroSwitch = view.findViewById(R.id.pomodoroSwitch)
        autoBackupSwitch = view.findViewById(R.id.autoBackupSwitch)
        soundEffectsSwitch = view.findViewById(R.id.soundEffectsSwitch)
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch)
        pomodoroWorkTimeSeekBar = view.findViewById(R.id.pomodoroWorkTimeSeekBar)
        pomodoroBreakTimeSeekBar = view.findViewById(R.id.pomodoroBreakTimeSeekBar)
        pomodoroWorkTimeText = view.findViewById(R.id.pomodoroWorkTimeText)
        pomodoroBreakTimeText = view.findViewById(R.id.pomodoroBreakTimeText)
        notificationTimeSpinner = view.findViewById(R.id.notificationTimeSpinner)
        exportDataButton = view.findViewById(R.id.exportDataButton)
        clearDataButton = view.findViewById(R.id.clearDataButton)
        resetSettingsButton = view.findViewById(R.id.resetSettingsButton)
        aboutButton = view.findViewById(R.id.aboutButton)
        
        // Setup notification reminder spinner
        val reminderOptions = arrayOf("15 minutes", "30 minutes", "1 hour", "2 hours", "Never")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, reminderOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        notificationTimeSpinner.adapter = spinnerAdapter
        
        // Setup seekbars
        pomodoroWorkTimeSeekBar.max = 60 // 5 to 65 minutes
        pomodoroWorkTimeSeekBar.progress = 20 // Default 25 minutes (20 + 5)
        pomodoroBreakTimeSeekBar.max = 25 // 5 to 30 minutes
        pomodoroBreakTimeSeekBar.progress = 0 // Default 5 minutes (0 + 5)
    }
    
    private fun loadSettings() {
        // Load settings from SharedPreferences
        notificationsEnabled = sharedPrefs.getBoolean(PREF_NOTIFICATIONS, true)
        pomodoroEnabled = sharedPrefs.getBoolean(PREF_POMODORO, false)
        autoBackupEnabled = sharedPrefs.getBoolean(PREF_AUTO_BACKUP, true)
        soundEffectsEnabled = sharedPrefs.getBoolean(PREF_SOUND_EFFECTS, true)
        darkModeEnabled = sharedPrefs.getBoolean(PREF_DARK_MODE, true)
        pomodoroWorkMinutes = sharedPrefs.getInt(PREF_POMODORO_WORK, 25)
        pomodoroBreakMinutes = sharedPrefs.getInt(PREF_POMODORO_BREAK, 5)
        notificationReminderMinutes = sharedPrefs.getInt(PREF_NOTIFICATION_REMINDER, 30)
        
        // Apply settings to UI
        notificationsSwitch.isChecked = notificationsEnabled
        pomodoroSwitch.isChecked = pomodoroEnabled
        autoBackupSwitch.isChecked = autoBackupEnabled
        soundEffectsSwitch.isChecked = soundEffectsEnabled
        darkModeSwitch.isChecked = darkModeEnabled
        
        pomodoroWorkTimeSeekBar.progress = pomodoroWorkMinutes - 5
        pomodoroBreakTimeSeekBar.progress = pomodoroBreakMinutes - 5
        
        updatePomodoroTimeTexts()
        
        // Set notification spinner selection
        val spinnerPosition = when (notificationReminderMinutes) {
            15 -> 0
            30 -> 1
            60 -> 2
            120 -> 3
            else -> 4 // Never
        }
        notificationTimeSpinner.setSelection(spinnerPosition)
    }
    
    private fun setupListeners() {
        // Switch listeners
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationsEnabled = isChecked
            saveSettings()
            if (isChecked) {
                Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        pomodoroSwitch.setOnCheckedChangeListener { _, isChecked ->
            pomodoroEnabled = isChecked
            saveSettings()
            togglePomodoroSettings(isChecked)
            if (isChecked) {
                Toast.makeText(context, "Pomodoro timer enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Pomodoro timer disabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        autoBackupSwitch.setOnCheckedChangeListener { _, isChecked ->
            autoBackupEnabled = isChecked
            saveSettings()
            if (isChecked) {
                Toast.makeText(context, "Auto backup enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Auto backup disabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        soundEffectsSwitch.setOnCheckedChangeListener { _, isChecked ->
            soundEffectsEnabled = isChecked
            saveSettings()
        }
        
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            darkModeEnabled = isChecked
            saveSettings()
            Toast.makeText(context, "Restart app to apply theme changes", Toast.LENGTH_SHORT).show()
        }
        
        // SeekBar listeners
        pomodoroWorkTimeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                pomodoroWorkMinutes = progress + 5
                updatePomodoroTimeTexts()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })
        
        pomodoroBreakTimeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                pomodoroBreakMinutes = progress + 5
                updatePomodoroTimeTexts()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })
        
        // Spinner listener
        notificationTimeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                notificationReminderMinutes = when (position) {
                    0 -> 15
                    1 -> 30
                    2 -> 60
                    3 -> 120
                    else -> -1 // Never
                }
                saveSettings()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Button listeners
        exportDataButton.setOnClickListener { showExportDataDialog() }
        clearDataButton.setOnClickListener { showClearDataDialog() }
        resetSettingsButton.setOnClickListener { showResetSettingsDialog() }
        aboutButton.setOnClickListener { showAboutDialog() }
        
        // Initialize pomodoro settings visibility
        togglePomodoroSettings(pomodoroEnabled)
    }
    
    private fun updatePomodoroTimeTexts() {
        pomodoroWorkTimeText.text = "$pomodoroWorkMinutes minutes"
        pomodoroBreakTimeText.text = "$pomodoroBreakMinutes minutes"
    }
    
    private fun togglePomodoroSettings(enabled: Boolean) {
        val visibility = if (enabled) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.pomodoroSettingsContainer)?.visibility = visibility
    }
    
    private fun saveSettings() {
        sharedPrefs.edit().apply {
            putBoolean(PREF_NOTIFICATIONS, notificationsEnabled)
            putBoolean(PREF_POMODORO, pomodoroEnabled)
            putBoolean(PREF_AUTO_BACKUP, autoBackupEnabled)
            putBoolean(PREF_SOUND_EFFECTS, soundEffectsEnabled)
            putBoolean(PREF_DARK_MODE, darkModeEnabled)
            putInt(PREF_POMODORO_WORK, pomodoroWorkMinutes)
            putInt(PREF_POMODORO_BREAK, pomodoroBreakMinutes)
            putInt(PREF_NOTIFICATION_REMINDER, notificationReminderMinutes)
            apply()
        }
    }
    
    private fun showExportDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Export Data")
            .setMessage("Export your study data and statistics to share or backup?")
            .setPositiveButton("Export") { _, _ ->
                exportStudyData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("⚠️ This will permanently delete all your study sessions, statistics, schedules, and goals. This action cannot be undone!\n\nAre you sure you want to continue?")
            .setPositiveButton("Delete All") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showResetSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Settings")
            .setMessage("Reset all settings to default values?")
            .setPositiveButton("Reset") { _, _ ->
                resetToDefaultSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About Study Stopwatch")
            .setMessage("""
                📚 Study Stopwatch v1.0
                
                A comprehensive productivity app designed to help you track study time, manage schedules, and achieve your learning goals.
                
                Features:
                • Activity-based time tracking
                • Comprehensive statistics
                • Schedule management
                • Goal setting and progress tracking
                • Pomodoro timer support
                • Data backup and export
                
                Developed for focused learning and productivity.
                
                © 2025 Study Stopwatch App
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun exportStudyData() {
        Toast.makeText(context, "Exporting data...", Toast.LENGTH_SHORT).show()
        
        // Export logic would go here
        // For now, show a success message
        Toast.makeText(context, "Data export feature coming soon!", Toast.LENGTH_LONG).show()
    }
    
    private fun clearAllData() {
        database.removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "All data cleared successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to clear data", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun resetToDefaultSettings() {
        sharedPrefs.edit().clear().apply()
        loadSettings()
        Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
    }
}
