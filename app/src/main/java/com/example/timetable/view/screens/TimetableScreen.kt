package com.example.timetable.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Settings
import com.example.timetable.view.components.timetable.LiveUpdateBanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetable.view.components.timetable.DailyView
import com.example.timetable.view.components.timetable.EventDetailOverlay
import com.example.timetable.view.components.timetable.TimetableGrid
import com.example.timetable.data.model.Lesson
import com.example.timetable.viewmodel.TimetableViewModel
import com.example.timetable.data.repository.RepositorySyncState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.timetable.utils.plusWeekdays
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

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
 * @param viewModel ViewModel zur Synchronisierung und Statusabfrage.
 * @param onNavigateToCourseSelection Callback-Methode zum Öffnen des Modul-Hinzufügen-Bildschirms.
 * @param onNavigateToSettings Callback-Methode zum Öffnen der App-Einstellungen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    courseName: String,
    viewModel: TimetableViewModel,
    onNavigateToCourseSelection: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val syncState by viewModel.syncState.collectAsState()
    val isRefreshing = syncState == RepositorySyncState.Syncing

    val userCalenderDays by viewModel.userCalenderDays.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val customEmojis = preferences.moduleEmojis

    val lessons = remember(userCalenderDays) {
        userCalenderDays.flatMap { it.lessons }
    }

    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var viewMode by remember { mutableStateOf(TimetableViewMode.WEEKLY) }

    val scope = rememberCoroutineScope()

    // dynamic reference date for the current day
    var today by remember { mutableStateOf(LocalDate.now()) }

    // update "today" if the app stays open past midnight
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDate.now()
            if (now != today) {
                today = now
            }
            kotlinx.coroutines.delay((1000 * 60).milliseconds) // Check every minute
        }
    }

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
            val isCancellation = it.change?.reasonType == "cancellation" ||
                    it.change?.caption?.contains("aus", ignoreCase = true) == true ||
                    it.change?.caption?.contains("cancell", ignoreCase = true) == true
            val msg =
                it.change?.modified?.ifBlank { it.change.caption.orEmpty() } ?: it.change?.caption
                ?: ""
            Pair(msg, isCancellation)
        }
    }
    var dismissedWarningMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // weekly/daily navigation bar selections
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .height(64.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
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
                    label = {
                        Text(
                            "Heute",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
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
                    label = {
                        Text(
                            "Woche",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
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
                // Custom compact top bar row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = courseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Dein Stundenplan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateToCourseSelection) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Module hinzufügen",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Einstellungen",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                // dynamic alerts banner displayed if changes are found
                if (activeWarning != null && activeWarning.first != dismissedWarningMessage) {
                    LiveUpdateBanner(
                        message = activeWarning.first,
                        isCancellation = activeWarning.second,
                        onDismiss = { dismissedWarningMessage = activeWarning.first }
                    )
                }

                // horizontal sliding containers switching active pages wrapped with PullToRefreshBox
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.triggerRefresh() },
                    modifier = Modifier.weight(1f)
                ) {
                    if (viewMode == TimetableViewMode.DAILY) {
                        HorizontalPager(
                            state = dayPagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val targetDate = today.plusWeekdays((page - INITIAL_PAGE).toLong())
                            val dayLessons = lessons.filter { it.date == targetDate.toString() }

                            DailyView(
                                date = targetDate,
                                lessons = dayLessons,
                                customEmojis = customEmojis,
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

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scrollable(
                                        state = rememberScrollableState { delta -> delta },
                                        orientation = Orientation.Vertical
                                    )
                            ) {
                                TimetableGrid(
                                    weekStart = weekStart,
                                    lessons = weekLessons,
                                    customEmojis = customEmojis,
                                    currentTime = if (weekOffset == 0L) LocalTime.now() else null,
                                    onLessonClick = { selectedLesson = it }
                                )
                            }
                        }
                    }
                }
            }

            // overlay displaying specific lesson event details
            EventDetailOverlay(
                lesson = selectedLesson,
                customEmoji = selectedLesson?.let { customEmojis[it.title] },
                bottomPadding = innerPadding.calculateBottomPadding(),
                onEmojiSelected = { emoji ->
                    selectedLesson?.let {
                        viewModel.updateModuleEmoji(it.title, emoji)
                    }
                },
                onRemoveClick = {
                    selectedLesson?.let { l ->
                        val primaryCode = preferences.groupsCode ?: ""
                        if (l.groupsCode.contains(primaryCode)) {
                            viewModel.hideModule(primaryCode, l.title)
                        } else {
                            viewModel.removeExtraModule(primaryCode, l.title)
                        }
                    }
                },
                onDismiss = { selectedLesson = null }
            )
        }
    }
}
