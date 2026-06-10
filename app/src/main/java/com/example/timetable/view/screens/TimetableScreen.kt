package com.example.timetable.view.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.view.components.EventDetailOverlay
import com.example.timetable.view.components.TimetableGrid
import com.example.timetable.view.components.TimetableLessonCard
import com.example.timetable.view.json.JsonLesson
import com.example.timetable.view.json.JsonLessonRepository

enum class TimetableViewMode {
    DAILY, WEEKLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    courseName: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { JsonLessonRepository(context) }
    val lessons = remember(courseName) { repository.getLessonsByCourse(courseName) }
    
    var selectedLesson by remember { mutableStateOf<JsonLesson?>(null) }
    var viewMode by remember { mutableStateOf(TimetableViewMode.DAILY) }
    
    // Group lessons by date
    val groupedLessons = remember(lessons) {
        lessons.groupBy { it.date }
    }

    Scaffold(
        topBar = {
            Column {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Dein Plan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (courseName.contains("- ")) courseName.substringAfter("- ").substringBefore(" (") else courseName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
                
                TabRow(
                    selectedTabIndex = viewMode.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = viewMode == TimetableViewMode.DAILY,
                        onClick = { viewMode = TimetableViewMode.DAILY },
                        text = { Text("Tagesansicht") }
                    )
                    Tab(
                        selected = viewMode == TimetableViewMode.WEEKLY,
                        onClick = { viewMode = TimetableViewMode.WEEKLY },
                        text = { Text("Wochenansicht") }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            if (viewMode == TimetableViewMode.DAILY) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    groupedLessons.forEach { (date, dailyLessons) ->
                        item {
                            Text(
                                text = date, // TODO: Format date nicely
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        itemsIndexed(dailyLessons) { index, lesson ->
                            var isVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                isVisible = true
                            }
                            
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = index * 50)) +
                                        slideInVertically(
                                            animationSpec = tween(durationMillis = 500, delayMillis = index * 50),
                                            initialOffsetY = { 40 }
                                        )
                            ) {
                                TimetableLessonCard(
                                    lesson = lesson,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = { selectedLesson = lesson }
                                )
                            }
                        }
                    }
                    
                    if (lessons.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Keine Vorlesungen für diesen Kurs gefunden.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.padding(innerPadding)) {
                    TimetableGrid(
                        lessons = lessons,
                        onLessonClick = { selectedLesson = it }
                    )
                }
            }
            
            // Overlay
            EventDetailOverlay(
                lesson = selectedLesson?.toLesson(),
                onDismiss = { selectedLesson = null }
            )
        }
    }
}
