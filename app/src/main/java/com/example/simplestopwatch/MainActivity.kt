package com.example.simplestopwatch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button

    private var handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var pausedTime = 0L
    private var isRunning = false

    private val runnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis() - startTime
            updateTimerText(currentTime)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        resetButton = findViewById(R.id.resetButton)

        startButton.setOnClickListener { startTimer() }
        pauseButton.setOnClickListener { pauseTimer() }
        resetButton.setOnClickListener {
            sendTimeToDatabase(pausedTime) {
                resetTimer()
            }
        }
    }

    private fun startTimer() {
        if (!isRunning) {
            startTime = System.currentTimeMillis() - pausedTime
            handler.postDelayed(runnable, 0)
            isRunning = true
            startButton.isEnabled = false
            pauseButton.isEnabled = true
            resetButton.isEnabled = false
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

    private fun resetTimer() {
        handler.removeCallbacks(runnable)
        timerTextView.text = "00:00:00"
        pausedTime = 0
        isRunning = false
        startButton.isEnabled = true
        pauseButton.isEnabled = false
        resetButton.isEnabled = false
    }

    private fun updateTimerText(millis: Long) {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun sendTimeToDatabase(millis: Long, onComplete: () -> Unit) {
        // Format the date
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Format the elapsed time into HH:mm:ss
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        val totalTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        // Get a reference to the Firebase Realtime Database
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("study_time")

        // Check if the date already exists in the database
        ref.child(date).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // If the date exists, update the total time
                val existingTime = snapshot.child("total_time").getValue(String::class.java) ?: "00:00:00"
                val newTime = addTimes(existingTime, totalTime)
                ref.child(date).child("total_time").setValue(newTime)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Time updated successfully", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error updating time", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
            } else {
                // If the date does not exist, insert a new row
                ref.child(date).child("total_time").setValue(totalTime)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Time inserted successfully", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error inserting time", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error checking record", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    private fun addTimes(time1: String, time2: String): String {
        val (h1, m1, s1) = time1.split(":").map { it.toInt() }
        val (h2, m2, s2) = time2.split(":").map { it.toInt() }

        var seconds = s1 + s2
        var minutes = m1 + m2 + seconds / 60
        val hours = h1 + h2 + minutes / 60

        seconds %= 60
        minutes %= 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}