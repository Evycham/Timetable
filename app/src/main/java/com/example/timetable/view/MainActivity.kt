package com.example.timetable.view

import java.util.concurrent.TimeUnit
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.example.timetable.view.components.common.AnimatedBackground
import com.example.timetable.view.navigation.TimetableNavHost
import com.example.timetable.view.theme.TimeTableTheme
import com.example.timetable.view.navigation.Screen
import androidx.work.WorkManager
import com.example.timetable.data.services.TimetableSyncWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // register periodic backdround-sync-worker (every 1.5hrs from 7..19 + random delay (max 5mins))
        val syncRequest = PeriodicWorkRequestBuilder<TimetableSyncWorker>(
            90, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "timetable_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        setContent {
            TimeTableTheme {
                // create navcontroller
                val navController = rememberNavController()
                // watch current nav-entry and route for wave animation onChange
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // route as key for animation on sceen change
                        AnimatedBackground(route = currentRoute)
                        // navController gets navhost
                        TimetableNavHost(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun NavScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HOSTvinci Debug Nav",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val buttonModifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp)

        Button(
            onClick = { navController.navigate(Screen.InitialSetup.route) },
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("1. Initial Setup Screen")
        }

        Button(
            onClick = { navController.navigate(Screen.Timetable.route) },
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("2. Timetable Main View")
        }

        Button(
            onClick = { navController.navigate(Screen.CourseSelection.route) },
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("3. Course Selection (Edit)")
        }
    }
}