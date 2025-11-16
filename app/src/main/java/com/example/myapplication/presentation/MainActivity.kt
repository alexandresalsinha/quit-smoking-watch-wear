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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import java.util.Calendar
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R

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
const val CIG_COUNT_KEY = "cig_smoked_count" // Renamed for clarity
const val CIG_LAST_RESET_TIME_KEY = "cig_last_reset_time" // Renamed for clarity
const val WEED_COUNT_KEY = "weed_smoked_count" // New key
const val WEED_LAST_RESET_TIME_KEY = "weed_last_reset_time" // New key

/**
 * Loads a specific smoked count from SharedPreferences.
 */
fun loadCount(context: Context, key: String): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(key, 0)
}

/**
 * Loads a specific last reset time (timestamp in milliseconds) from SharedPreferences.
 */
fun loadLastResetTime(context: Context, key: String): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(key, 0L)
}

/**
 * Saves a specific smoked count and its corresponding last action time to SharedPreferences.
 */
fun saveSmokedData(context: Context, countKey: String, count: Int, timeKey: String, currentTime: Long) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putInt(countKey, count)
        .putLong(timeKey, currentTime)
        .apply()
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

    // 2. States for both counters, initialized by loading from storage
    var cigCount by remember { mutableStateOf(loadCount(context, CIG_COUNT_KEY)) }
    var cigLastResetTime by remember { mutableStateOf(loadLastResetTime(context, CIG_LAST_RESET_TIME_KEY)) }

    var weedCount by remember { mutableStateOf(loadCount(context, WEED_COUNT_KEY)) } // NEW weed count
    var weedLastResetTime by remember { mutableStateOf(loadLastResetTime(context, WEED_LAST_RESET_TIME_KEY)) } // NEW weed last reset time

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

    // --- Daily Auto-Reset Logic for Cigarette Counter ---
    LaunchedEffect(Unit) { // This effect runs once when the composable enters the composition
        val now = Calendar.getInstance()
        val today9AM = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // --- Cigarette Count Auto-Reset ---
        if (now.timeInMillis > today9AM.timeInMillis) {
            if (cigLastResetTime < today9AM.timeInMillis) {
                if (cigCount > 0) {
                    val currentTime = System.currentTimeMillis()
                    cigCount = 0
                    cigLastResetTime = currentTime
                    saveSmokedData(context, CIG_COUNT_KEY, 0, CIG_LAST_RESET_TIME_KEY, currentTime)
                }
            }
        }

        // --- Weed Count Auto-Reset (same logic, independent check) ---
        if (now.timeInMillis > today9AM.timeInMillis) {
            if (weedLastResetTime < today9AM.timeInMillis) {
                if (weedCount > 0) {
                    val currentTime = System.currentTimeMillis()
                    weedCount = 0
                    weedLastResetTime = currentTime
                    saveSmokedData(context, WEED_COUNT_KEY, 0, WEED_LAST_RESET_TIME_KEY, currentTime)
                }
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier.background(Color.Black)
    ) {
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
                style = MaterialTheme.typography.title1.copy(fontWeight = FontWeight.ExtraBold),
                color = if (timeSeconds > 60) Color.White else Color(0xFFFFCC00),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- Buttons Row ---
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly, // Distribute buttons evenly
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp) // Add slight horizontal padding for better spacing
            ) {
                // Cigarette Button
                Button(
                    onClick = {
                        timeSeconds = INITIAL_TIME_SECONDS
                        isRunning = true

                        val newCount = cigCount + 1
                        cigCount = newCount

                        val currentTime = System.currentTimeMillis()
                        cigLastResetTime = currentTime
                        saveSmokedData(context, CIG_COUNT_KEY, newCount, CIG_LAST_RESET_TIME_KEY, currentTime)
                    },
                    colors = ButtonDefaults.secondaryButtonColors(
                        backgroundColor = Color(0xFFE53935) // Vibrant Red
                    ),
                    enabled = true,
                    modifier = Modifier
                        .weight(1f) // Make button take up available space
                        .height(60.dp)
                        .padding(end = 4.dp) // Padding between buttons
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cigarette_icon),
                        contentDescription = "Cigarette Smoked",
                        modifier = Modifier.size(36.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                    )
                }

                // --- NEW Marijuana Button ---
                Button(
                    onClick = {
                        timeSeconds = INITIAL_TIME_SECONDS
                        isRunning = true

                        val newCount = weedCount + 1
                        weedCount = newCount

                        val currentTime = System.currentTimeMillis()
                        weedLastResetTime = currentTime
                        saveSmokedData(context, WEED_COUNT_KEY, newCount, WEED_LAST_RESET_TIME_KEY, currentTime)
                    },
                    colors = ButtonDefaults.secondaryButtonColors(
                        backgroundColor = Color(0xFF4CAF50) // Green
                    ),
                    enabled = true,
                    modifier = Modifier
                        .weight(1f) // Make button take up available space
                        .height(60.dp)
                        .padding(start = 4.dp) // Padding between buttons
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.marijuana_icon), // Placeholder
                        contentDescription = "Marijuana Smoked",
                        modifier = Modifier.size(36.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                    )
                }
            }
            // --- End Buttons Row ---

            Spacer(modifier = Modifier.height(8.dp))

            // Counter Displays - NEW
            Text(
                text = "Cig. Count : $cigCount",
                style = MaterialTheme.typography.body1,
                color = Color.Gray,
            )
            Text(
                text = "Weed Count: $weedCount",
                style = MaterialTheme.typography.body1,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp) // Add a little space between counters
            )
        }
    }
}