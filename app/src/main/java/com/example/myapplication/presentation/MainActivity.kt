package com.example.myapplication.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Added for accessing SharedPreferences
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Main Activity for the Wear OS app.
 * This app implements a simple countdown timer using Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

// --- Persistence Constants and Functions ---
const val PREFS_NAME = "SmokedPrefs"
const val COUNT_KEY = "smoked_count"

/**
 * Loads the smoked count from SharedPreferences. Returns 0 if no count is found.
 */
fun loadSmokedCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(COUNT_KEY, 0)
}

/**
 * Saves the smoked count to SharedPreferences.
 */
fun saveSmokedCount(context: Context, count: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(COUNT_KEY, count).apply()
}
// --- End Persistence Functions ---


/**
 * Converts total seconds into a readable HH:MM:SS format.
 */
fun formatTime(totalSeconds: Long): String {
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60

    // Format as HH:MM:SS
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
fun WearApp() {
    // Get the current context to access SharedPreferences
    val context = LocalContext.current

    // 1 hour (3600s) + 15 minutes (900s) = 4500 seconds
    val INITIAL_TIME_SECONDS = 4500L

    // 1. State for the countdown timer
    var timeSeconds by remember { mutableStateOf(INITIAL_TIME_SECONDS) }

    // Timer starts in a paused state when the app loads
    var isRunning by remember { mutableStateOf(false) }

    // 2. State for the counter, initialized by loading from storage
    // *** MODIFIED HERE: Load initial count from storage ***
    var smokedCount by remember { mutableStateOf(loadSmokedCount(context)) }

    // Coroutine to handle the countdown logic
    LaunchedEffect(isRunning) {
        // Continue running if the timer is active AND time is greater than zero
        while (isRunning && timeSeconds > 0) {
            delay(1000L) // Wait for 1 second
            timeSeconds-- // DECREMENT time
        }

        // Automatically stop running if time hits zero
        if (timeSeconds == 0L) {
            isRunning = false
        }
    }

    Scaffold(
        // Displays the current time at the top of the screen
        timeText = { TimeText() },
        modifier = Modifier.background(Color.Black)
    ) {
        // Use a Column to stack the Timer Display and the Controls vertically
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Timer Display
            Text(
                text = formatTime(timeSeconds),
                // CHANGED: Using title1 typography for a smaller size
                style = MaterialTheme.typography.title1.copy(fontWeight = FontWeight.ExtraBold),
                // Change color in the last minute (60 seconds)
                color = if (timeSeconds > 60) Color.White else Color(0xFFFFCC00),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Controls (Only the Reset Button remains)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Reset Button ("Smoked")
                Button(
                    onClick = {
                        // Reset to initial time (1h 15m) and start running immediately
                        timeSeconds = INITIAL_TIME_SECONDS
                        isRunning = true // Starts the countdown

                        // Increment state
                        val newCount = smokedCount + 1
                        smokedCount = newCount

                        // *** MODIFIED HERE: Save the new count to storage ***
                        saveSmokedCount(context, newCount)
                    },
                    colors = ButtonDefaults.secondaryButtonColors(
                        backgroundColor = Color(0xFFE53935) // Vibrant Red
                    ),
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    // Set the text to "Smoked"
                    Text("Smoked")
                }
            }

            // Add some spacing between button and counter
            Spacer(modifier = Modifier.height(8.dp))

            // Counter Display - NEW
            Text(
                text = "Smoked Count: $smokedCount",
                style = MaterialTheme.typography.body1,
                color = Color.Gray,
            )
        }
    }
}