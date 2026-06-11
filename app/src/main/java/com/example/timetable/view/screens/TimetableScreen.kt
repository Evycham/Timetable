package com.example.timetable.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Settings
import com.example.timetable.view.components.timetable.LiveUpdateBanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.view.components.timetable.DailyView
import com.example.timetable.view.components.timetable.EventDetailOverlay
import com.example.timetable.view.components.timetable.TimetableGrid
import com.example.timetable.view.json.JsonLesson
import com.example.timetable.view.json.JsonLessonRepository
import com.example.timetable.view.json.MockLogic
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Bestimmt das Anzeigeformat des Stundenplans auf dem Bildschirm.
 */
enum class TimetableViewMode {
    DAILY, WEEKLY
}

private const val INITIAL_PAGE = 500
private const val PAGE_COUNT = 1000

/**
 * Der Hauptbildschirm für die Anzeige des Stundenplans.
 * Unterstützt die Umschaltung zwischen der Tagesansicht (Heute) und der Wochenansicht (Woche),
 * das Filtern und Suchen von Modulen, den Schnellzugriff auf Einstellungen sowie die Anzeige
 * von aktuellen Ausfall- und Raumhinweisen über ein interaktives Banner.
 *
 * @param courseName Name des ausgewählten Studiengangs oder Kurses.
 * @param onNavigateBack Callback-Methode zur Rückkehr zur vorherigen Ansicht.
 * @param onNavigateToCourseSelection Callback-Methode zum Öffnen des Modul-Hinzufügen-Bildschirms.
 * @param onNavigateToSettings Callback-Methode zum Öffnen der App-Einstellungen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    courseName: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToCourseSelection: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    // load repository and resolve active lessons from selected modules
    val context = LocalContext.current
    val repository = remember { JsonLessonRepository(context) }

    // resolve active faculty color from courseName prefix
    LaunchedEffect(courseName) {
        val facultyColor = when {
            courseName.startsWith("eti-", ignoreCase = true) -> Color(0xff00bfff)
            courseName.startsWith("mb-", ignoreCase = true) -> Color(0xffffd700)
            courseName.startsWith("ws-", ignoreCase = true) -> Color(0xff008080)
            else -> null
        }
        MockLogic.activeFacultyColor = facultyColor
    }

    val lessons by remember {
        derivedStateOf {
            MockLogic.selectedModuleTitles.flatMap { title ->
                repository.getLessonsByModuleTitle(title)
            }
        }
    }

    var selectedLesson by remember { mutableStateOf<JsonLesson?>(null) }
    var viewMode by remember { mutableStateOf(TimetableViewMode.WEEKLY) }

    val scope = rememberCoroutineScope()

    // static baseline reference date for sample dataset
    val today = LocalDate.of(2026, 6, 15)

    // page controller configuration for swiping days/weeks
    val dayPagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { PAGE_COUNT }
    )

    val weekPagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { PAGE_COUNT }
    )

    // filter lessons containing any live updates or cancellations
    val activeWarning = remember(lessons) {
        lessons.firstOrNull { it.change != null }?.let {
            val isCancellation = it.change?.caption?.contains("aus", ignoreCase = true) == true ||
                    it.change?.message?.contains("aus", ignoreCase = true) == true ||
                    it.change?.caption?.contains("cancell", ignoreCase = true) == true
            val msg = it.change?.message?.ifBlank { it.change.caption } ?: it.change?.caption ?: ""
            Pair(msg, isCancellation)
        }
    }
    var dismissedWarningMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // top bar navigation and actions
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCourseSelection) {
                        Icon(Icons.Default.Add, contentDescription = "Module hinzufügen")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            // weekly/daily navigation bar selections
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 8.dp
            ) {
                val dailyActive = viewMode == TimetableViewMode.DAILY
                NavigationBarItem(
                    selected = dailyActive,
                    onClick = {
                        if (dailyActive) {
                            scope.launch { dayPagerState.animateScrollToPage(INITIAL_PAGE) }
                        } else {
                            viewMode = TimetableViewMode.DAILY
                        }
                    },
                    label = { Text("Heute", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarViewDay,
                            contentDescription = "Heute"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                val weeklyActive = viewMode == TimetableViewMode.WEEKLY
                NavigationBarItem(
                    selected = weeklyActive,
                    onClick = {
                        if (weeklyActive) {
                            scope.launch { weekPagerState.animateScrollToPage(INITIAL_PAGE) }
                        } else {
                            viewMode = TimetableViewMode.WEEKLY
                        }
                    },
                    label = { Text("Woche", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarViewWeek,
                            contentDescription = "Woche"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // dynamic alerts banner displayed if changes are found
                if (activeWarning != null && activeWarning.first != dismissedWarningMessage) {
                    LiveUpdateBanner(
                        message = activeWarning.first,
                        isCancellation = activeWarning.second,
                        onDismiss = { dismissedWarningMessage = activeWarning.first }
                    )
                }

                // horizontal sliding containers switching active pages
                Box(modifier = Modifier.weight(1f)) {
                    if (viewMode == TimetableViewMode.DAILY) {
                        HorizontalPager(
                            state = dayPagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val targetDate = today.plusDays((page - INITIAL_PAGE).toLong())
                            val dayLessons = lessons.filter { it.date == targetDate.toString() }

                            DailyView(
                                date = targetDate,
                                lessons = dayLessons,
                                onLessonClick = { selectedLesson = it }
                            )
                        }
                    } else {
                        HorizontalPager(
                            state = weekPagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val weekOffset = (page - INITIAL_PAGE).toLong()
                            val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                                .plusWeeks(weekOffset)
                            val weekEnd = weekStart.plusDays(6)

                            val weekLessons = lessons.filter {
                                try {
                                    val lessonDate = LocalDate.parse(it.date)
                                    !lessonDate.isBefore(weekStart) && !lessonDate.isAfter(weekEnd)
                                } catch (_: Exception) {
                                    false
                                }
                            }

                            TimetableGrid(
                                lessons = weekLessons,
                                currentTime = if (weekOffset == 0L) LocalTime.now() else null,
                                onLessonClick = { selectedLesson = it }
                            )
                        }
                    }
                }
            }

            // overlay displaying specific lesson event details
            EventDetailOverlay(
                lesson = selectedLesson?.toLesson(),
                bottomPadding = innerPadding.calculateBottomPadding(),
                onDismiss = { selectedLesson = null }
            )
        }
    }
}
