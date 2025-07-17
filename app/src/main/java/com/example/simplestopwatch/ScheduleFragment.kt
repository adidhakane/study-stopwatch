package com.example.simplestopwatch

import android.app.TimePickerDialog
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

class ScheduleFragment : Fragment() {
    
    private lateinit var database: DatabaseReference
    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var todayScheduleRecyclerView: RecyclerView
    private lateinit var addScheduleButton: FloatingActionButton
    private lateinit var daySpinner: Spinner
    private lateinit var adherenceProgressBar: ProgressBar
    private lateinit var adherencePercentageText: TextView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var todayScheduleAdapter: TodayScheduleAdapter
    
    private val allScheduleItems = mutableListOf<ScheduleItem>()
    private val todayScheduleItems = mutableListOf<TodayScheduleItem>()
    private val daysOfWeek = arrayOf("All Days", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerViews()
        setupSpinner()
        initializeDatabase()
        loadScheduleData()
        loadTodaySchedule()
        calculateTodayAdherence()
    }
    
    private fun initializeViews(view: View) {
        scheduleRecyclerView = view.findViewById(R.id.scheduleRecyclerView)
        todayScheduleRecyclerView = view.findViewById(R.id.todayScheduleRecyclerView)
        addScheduleButton = view.findViewById(R.id.addScheduleButton)
        daySpinner = view.findViewById(R.id.daySpinner)
        adherenceProgressBar = view.findViewById(R.id.adherenceProgressBar)
        adherencePercentageText = view.findViewById(R.id.adherencePercentageText)
        
        addScheduleButton.setOnClickListener {
            showAddScheduleDialog()
        }
    }
    
    private fun setupRecyclerViews() {
        scheduleAdapter = ScheduleAdapter(allScheduleItems) { scheduleItem ->
            showEditScheduleDialog(scheduleItem)
        }
        
        scheduleRecyclerView.apply {
            adapter = scheduleAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        todayScheduleAdapter = TodayScheduleAdapter(todayScheduleItems) { todayItem ->
            markAsCompleted(todayItem)
        }
        
        todayScheduleRecyclerView.apply {
            adapter = todayScheduleAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        daySpinner.adapter = adapter
        
        daySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterScheduleByDay(position)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun initializeDatabase() {
        database = FirebaseDatabase.getInstance().reference
    }
    
    private fun loadScheduleData() {
        database.child("schedule_items")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allScheduleItems.clear()
                    
                    for (itemSnapshot in snapshot.children) {
                        val scheduleItem = itemSnapshot.getValue(ScheduleItem::class.java)
                        scheduleItem?.let { allScheduleItems.add(it) }
                    }
                    
                    allScheduleItems.sortBy { it.startTime }
                    filterScheduleByDay(daySpinner.selectedItemPosition)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load schedule", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun loadTodaySchedule() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayKey = when(today) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        
        database.child("schedule_items")
            .orderByChild("isActive")
            .equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    todayScheduleItems.clear()
                    
                    for (itemSnapshot in snapshot.children) {
                        val scheduleItem = itemSnapshot.getValue(ScheduleItem::class.java)
                        scheduleItem?.let { item ->
                            if (item.daysOfWeek.contains(todayKey)) {
                                val todayItem = TodayScheduleItem(
                                    scheduleItem = item,
                                    isCompleted = false,
                                    actualStartTime = null,
                                    actualEndTime = null,
                                    delayMinutes = 0,
                                    status = getScheduleStatus(item)
                                )
                                todayScheduleItems.add(todayItem)
                            }
                        }
                    }
                    
                    todayScheduleItems.sortBy { it.scheduleItem.startTime }
                    todayScheduleAdapter.notifyDataSetChanged()
                    calculateTodayAdherence()
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    private fun filterScheduleByDay(dayPosition: Int) {
        val filteredItems = if (dayPosition == 0) {
            allScheduleItems
        } else {
            allScheduleItems.filter { it.daysOfWeek.contains(dayPosition) }
        }
        
        scheduleAdapter.updateItems(filteredItems)
    }
    
    private fun showAddScheduleDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_schedule, null)
        
        val titleEdit = dialogView.findViewById<EditText>(R.id.titleEditText)
        val startTimeButton = dialogView.findViewById<Button>(R.id.startTimeButton)
        val endTimeButton = dialogView.findViewById<Button>(R.id.endTimeButton)
        val activityTypeSpinner = dialogView.findViewById<Spinner>(R.id.activityTypeSpinner)
        val flexibleCheckBox = dialogView.findViewById<CheckBox>(R.id.flexibleCheckBox)
        val toleranceEdit = dialogView.findViewById<EditText>(R.id.toleranceEditText)
        val reminderEdit = dialogView.findViewById<EditText>(R.id.reminderEditText)
        
        var startTime = ""
        var endTime = ""
        
        // Setup activity type spinner
        val activityTypes = arrayOf("Study", "Timepass")
        val activityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, activityTypes)
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        activityTypeSpinner.adapter = activityAdapter
        
        startTimeButton.setOnClickListener {
            showTimePickerDialog { time ->
                startTime = time
                startTimeButton.text = time
            }
        }
        
        endTimeButton.setOnClickListener {
            showTimePickerDialog { time ->
                endTime = time
                endTimeButton.text = time
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Schedule Item")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val scheduleItem = ScheduleItem(
                    id = database.child("schedule_items").push().key ?: "",
                    title = titleEdit.text.toString(),
                    startTime = startTime,
                    endTime = endTime,
                    activityType = if (activityTypeSpinner.selectedItemPosition == 0) ActivityType.STUDY else ActivityType.TIMEPASS,
                    isFlexible = flexibleCheckBox.isChecked,
                    toleranceMinutes = toleranceEdit.text.toString().toIntOrNull() ?: 30,
                    reminderMinutes = reminderEdit.text.toString().toIntOrNull() ?: 15,
                    daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7) // Default to all days
                )
                
                saveScheduleItem(scheduleItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditScheduleDialog(scheduleItem: ScheduleItem) {
        // Similar to add dialog but with pre-filled values
        Toast.makeText(context, "Edit schedule feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showTimePickerDialog(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(timeString)
        }, hour, minute, true).show()
    }
    
    private fun saveScheduleItem(scheduleItem: ScheduleItem) {
        database.child("schedule_items").child(scheduleItem.id).setValue(scheduleItem)
            .addOnSuccessListener {
                Toast.makeText(context, "Schedule item added successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add schedule item", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun markAsCompleted(todayItem: TodayScheduleItem) {
        val updatedItem = todayItem.copy(
            isCompleted = true,
            actualStartTime = System.currentTimeMillis(),
            actualEndTime = System.currentTimeMillis()
        )
        
        val index = todayScheduleItems.indexOf(todayItem)
        if (index != -1) {
            todayScheduleItems[index] = updatedItem
            todayScheduleAdapter.notifyItemChanged(index)
            calculateTodayAdherence()
        }
        
        // Save to Firebase
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        database.child("daily_schedule_completion")
            .child(dateKey)
            .child(todayItem.scheduleItem.id)
            .setValue(updatedItem)
    }
    
    private fun getScheduleStatus(item: ScheduleItem): ScheduleStatus {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute
        
        val startTimeParts = item.startTime.split(":")
        val startTimeMinutes = startTimeParts[0].toInt() * 60 + startTimeParts[1].toInt()
        
        val endTimeParts = item.endTime.split(":")
        val endTimeMinutes = endTimeParts[0].toInt() * 60 + endTimeParts[1].toInt()
        
        return when {
            currentTimeMinutes < startTimeMinutes - item.reminderMinutes -> ScheduleStatus.UPCOMING
            currentTimeMinutes < startTimeMinutes -> ScheduleStatus.REMINDER
            currentTimeMinutes <= endTimeMinutes -> ScheduleStatus.ACTIVE
            currentTimeMinutes <= endTimeMinutes + item.toleranceMinutes -> ScheduleStatus.OVERDUE
            else -> ScheduleStatus.MISSED
        }
    }
    
    private fun calculateTodayAdherence() {
        if (todayScheduleItems.isEmpty()) {
            adherenceProgressBar.progress = 100
            adherencePercentageText.text = "100%"
            return
        }
        
        val completedItems = todayScheduleItems.count { it.isCompleted }
        val totalItems = todayScheduleItems.size
        val percentage = (completedItems.toFloat() / totalItems * 100).toInt()
        
        adherenceProgressBar.progress = percentage
        adherencePercentageText.text = "$percentage%"
    }
}

// Data classes for schedule management
data class TodayScheduleItem(
    val scheduleItem: ScheduleItem,
    val isCompleted: Boolean,
    val actualStartTime: Long?,
    val actualEndTime: Long?,
    val delayMinutes: Int,
    val status: ScheduleStatus
)

enum class ScheduleStatus {
    UPCOMING, REMINDER, ACTIVE, OVERDUE, MISSED, COMPLETED
}
