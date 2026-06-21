package com.example.timetable.view

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import java.util.concurrent.TimeUnit
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.example.timetable.view.components.common.AnimatedBackground
import com.example.timetable.view.navigation.TimetableNavHost
import com.example.timetable.view.theme.TimeTableTheme
import androidx.work.WorkManager
import com.example.timetable.data.services.TimetableSyncWorker

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // we lock the screen to portrait mode.. android does not like this, so i am suppressing the warning
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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