package com.example.timetable.view.components.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetable.data.model.Lesson
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun DailyView(
    date: LocalDate,
    lessons: List<Lesson>,
    customEmojis: Map<String, String> = emptyMap(),
    onLessonClick: (Lesson) -> Unit
) {
    val now = LocalTime.now()
    val isToday = date == LocalDate.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            val formattedDate =
                date.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM", Locale.GERMAN))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (lessons.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(0.6f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Vorlesungen geplant.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        itemsIndexed(lessons.sortedBy { it.startTime }) { index, lesson ->
            val endTime = try {
                LocalTime.parse(lesson.endTime)
            } catch (_: Exception) {
                null
            }
            val isPast = isToday && endTime != null && endTime.isBefore(now)

            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isVisible = true }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 500,
                        delayMillis = index * 50
                    )
                ) +
                        slideInVertically(
                            animationSpec = tween(durationMillis = 500, delayMillis = index * 50),
                            initialOffsetY = { 40 }
                        )
            ) {
                TimetableLessonCard(
                    lesson = lesson,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isPast = isPast,
                    customEmoji = customEmojis[lesson.title],
                    onClick = { onLessonClick(lesson) }
                )
            }
        }
    }
}
