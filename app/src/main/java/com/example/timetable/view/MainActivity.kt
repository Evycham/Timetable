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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.example.timetable.view.components.common.AnimatedBackground
import com.example.timetable.view.navigation.TimetableNavHost
import com.example.timetable.view.navigation.Screen
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

private sealed interface StartupState {
    object Checking : StartupState
    object SplashAnimating : StartupState
    object Ready : StartupState
    object FirstLaunchLoading : StartupState
    data class FirstLaunchError(val message: String) : StartupState
}

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
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
        
        var startupState by mutableStateOf<StartupState>(StartupState.Checking)
        var initialPreferences by mutableStateOf<UserSchedulePreferences?>(null)

        splashScreen.setKeepOnScreenCondition {
            startupState == StartupState.Checking
        }

        fun startLoad() {
            lifecycleScope.launch {
                try {
                    val prefDeferred = async { preferencesStore.preferencesFlow.first() }
                    
                    if (repository.hasDatabaseData()) {
                        // 1. Lokale Daten laden
                        repository.initialize()
                        initialPreferences = prefDeferred.await()
                        
                        // 2. Native Splash schließen, Compose Premium-Splash-Animation starten
                        startupState = StartupState.SplashAnimating

                        // 3. Netzwerk-Update asynchron im Hintergrund prüfen
                        launch {
                            try {
                                repository.updateJsonIfNeeded()
                            } catch (_: Exception) {
                                // Fehler im Hintergrund ignorieren
                            }
                        }
                    } else {
                        // Erststart: Keine lokalen Daten
                        initialPreferences = prefDeferred.await()
                        startupState = StartupState.FirstLaunchLoading
                        repository.initialize() // API Download
                        startupState = StartupState.Ready
                    }
                } catch (e: Exception) {
                    if (startupState == StartupState.FirstLaunchLoading) {
                        startupState = StartupState.FirstLaunchError("Wir konnten die Stundenplandaten noch nicht herunterladen. Bitte Internetverbindung prüfen.")
                    } else {
                        startupState = StartupState.Ready
                    }
                }
            }
        }

        startLoad()

        setContent {
            val preferences by preferencesStore.preferencesFlow.collectAsState(
                initial = initialPreferences ?: UserSchedulePreferences()
            )
            val dynamicColor = preferences.isDynamicColorEnabled
            val appFontSize = preferences.appFontSize
            val backgroundAccentColor = remember { mutableStateOf<Color?>(null) }

            CompositionLocalProvider(LocalBackgroundAccentColor provides backgroundAccentColor) {
                TimeTableTheme(
                    dynamicColor = dynamicColor,
                    appFontSize = appFontSize
                ) {
                    val navController = rememberNavController()
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

                    val startRoute = remember(preferences) {
                        val groupsCode = preferences.groupsCode
                        if (preferences.isSetupComplete && !groupsCode.isNullOrBlank()) {
                            Screen.Timetable.createRoute(groupsCode)
                        } else {
                            Screen.InitialSetup.route
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (val state = startupState) {
                            is StartupState.Ready -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AnimatedBackground(route = currentRoute)
                                    TimetableNavHost(
                                        navController = navController,
                                        repository = repository,
                                        userService = userService,
                                        startDestination = startRoute
                                    )
                                }
                            }
                            StartupState.SplashAnimating -> {
                                PremiumSplashScreen(
                                    onAnimationFinished = {
                                        startupState = StartupState.Ready
                                    }
                                )
                            }
                            is StartupState.FirstLaunchLoading -> {
                                PrepareStartupScreen(
                                    isLoading = true,
                                    errorMessage = null,
                                    onRetry = {}
                                )
                            }
                            is StartupState.FirstLaunchError -> {
                                PrepareStartupScreen(
                                    isLoading = false,
                                    errorMessage = state.message,
                                    onRetry = {
                                        startupState = StartupState.Checking
                                        startLoad()
                                    }
                                )
                            }
                            StartupState.Checking -> {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSplashScreen(
    onAnimationFinished: () -> Unit
) {
    val background = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF1744),
            Color(0xFFD50032)
        )
    )

    var startAnimation by remember { mutableStateOf(false) }

    // Spring scaling for logo (starts exactly at 76dp/160dp = 0.475f to match native size)
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.475f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    // Offset and fade in for App Title "HOSTvinci"
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300),
        label = "TextAlpha"
    )

    val textOffsetY by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 800, delayMillis = 300),
        label = "TextOffset"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1200) // Keep the dynamic animation running for 1.2 seconds
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Logo is aligned at the absolute center, matching the OS Splash Screen position
        Image(
            painter = painterResource(R.drawable.splash_logo_image),
            contentDescription = "HOSTvinci Logo",
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.Center)
                .graphicsLayer(
                    scaleX = logoScale,
                    scaleY = logoScale
                ),
            contentScale = ContentScale.Fit
        )
        
        // Text is aligned to center but offset downwards, independent of the logo position
        Text(
            text = "HOSTvinci",
            color = Color.White,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 110.dp + textOffsetY)
                .graphicsLayer(alpha = textAlpha)
        )
    }
}

@Composable
private fun PrepareStartupScreen(
    isLoading: Boolean,
    errorMessage: String?,
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
            if (isLoading) {
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
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFD50032)
                    )
                ) {
                    Text("Erneut versuchen")
                }
            }
        }
    }
}
