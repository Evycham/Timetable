package com.example.timetable.view

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import java.util.concurrent.TimeUnit
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.runtime.collectAsState
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetable.view.theme.LocalBackgroundAccentColor
import com.example.timetable.R

private sealed interface StartupUiState {
    data object Preparing : StartupUiState
    data object Ready : StartupUiState
    data class Error(val message: String) : StartupUiState
}

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

        val preferencesStore = UserSchedulePreferencesStore(applicationContext.userSchedulePreferencesDataStore)
        val repository = TimetableRepository(applicationContext)
        val userService = UserTimetableService(
            repository = repository,
            preferencesStore = preferencesStore,
            database = com.example.timetable.data.local.db.TimetableDatabase.getInstance(applicationContext)
        )
        var startupUiState by mutableStateOf<StartupUiState>(StartupUiState.Preparing)

        lifecycleScope.launch {
            startupUiState = try {
                repository.prepareLaunchData()
                StartupUiState.Ready
            } catch (_: Exception) {
                StartupUiState.Error("Wir konnten die Stundenplandaten noch nicht herunterladen.")
            }
        }

        setContent {
            val preferences by preferencesStore.preferencesFlow.collectAsState(initial = UserSchedulePreferences())
            val dynamicColor = preferences.isDynamicColorEnabled
            val appFontSize = preferences.appFontSize
            val backgroundAccentColor = remember { mutableStateOf<Color?>(null) }

            CompositionLocalProvider(LocalBackgroundAccentColor provides backgroundAccentColor) {
                TimeTableTheme(
                    dynamicColor = dynamicColor,
                    appFontSize = appFontSize
                ) {
                    // create navcontroller
                    val navController = rememberNavController()
                    // watch current nav-entry and route for wave animation onChange
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (startupUiState == StartupUiState.Ready) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimatedBackground(route = currentRoute)
                                TimetableNavHost(
                                    navController = navController,
                                    repository = repository,
                                    userService = userService
                                )
                            }
                        } else {
                            PrepareStartupScreen(
                                startupUiState = startupUiState,
                                onRetry = {
                                    startupUiState = StartupUiState.Preparing
                                    lifecycleScope.launch {
                                        startupUiState = try {
                                            repository.prepareLaunchData()
                                            StartupUiState.Ready
                                        } catch (_: Exception) {
                                            StartupUiState.Error("Wir konnten die Stundenplandaten noch nicht herunterladen.")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@androidx.compose.runtime.Composable
private fun PrepareStartupScreen(
    startupUiState: StartupUiState,
    onRetry: () -> Unit
) {
    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF1744),
            Color(0xFFD50032)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.splash_logo_image),
                contentDescription = "HOSTvinci Logo",
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit
            )
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 4.dp
            )
            Text(
                text = "Wir bereiten alles vor",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (startupUiState is StartupUiState.Error) {
                Text(
                    text = startupUiState.message,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onRetry) {
                    Text("Erneut versuchen")
                }
            }
        }
    }
}
