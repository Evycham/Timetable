package com.example.timetable.view.components.timetable

import com.example.timetable.view.components.common.EmptyStateView

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.view.json.JsonLesson
import com.example.timetable.view.json.MockLogic
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Stellt den Stundenplan als Wochenraster (Montag bis Freitag) dar.
 * Die Vorlesungen werden an ihrer entsprechenden Uhrzeit und Wochentag
 * positioniert. Zudem wird eine aktuelle Zeitlinie angezeigt, wenn die
 * angezeigte Woche mit der aktuellen Kalenderwoche übereinstimmt.
 *
 * @param lessons Die anzuzeigenden Vorlesungen der Woche.
 * @param currentTime Die aktuelle Uhrzeit für die rote Zeitlinie. Wenn `null`, wird keine Linie gerendert.
 * @param onLessonClick Callback-Methode bei Klick auf einen Vorlesungseintrag.
 */
@Composable
fun TimetableGrid(
    lessons: List<JsonLesson>,
    currentTime: LocalTime? = null,
    onLessonClick: (JsonLesson) -> Unit
) {
    val days = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    // configure baseline hours for daily display range (08:00 to 19:00)
    val startHour = 8
    val endHour = 18 // last hour starts at 18:00 and ends at 19:00
    val totalHours = 11 // 11 hours total display range

    Column(modifier = Modifier.fillMaxSize()) {
        // Weekday Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            // weekday column headers rendering in german locale
            val dayNames = mapOf(
                DayOfWeek.MONDAY to "Mo",
                DayOfWeek.TUESDAY to "Di",
                DayOfWeek.WEDNESDAY to "Mi",
                DayOfWeek.THURSDAY to "Do",
                DayOfWeek.FRIDAY to "Fr"
            )
            days.forEach { day ->
                Text(
                    text = dayNames[day] ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // static Grid filling available height without scrolling
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val hourHeight = maxHeight / totalHours

            // time Labels & Grid Lines
            Column(modifier = Modifier.fillMaxSize()) {
                for (hour in startHour..endHour) {
                    Box(
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "%02d".format(hour),
                            modifier = Modifier
                                .width(40.dp)
                                .padding(end = 8.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )

                        // background grid lines drawing hour markers
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                .align(Alignment.TopStart)
                        )

                        // draw bottom 19:00 border line and label at the bottom of 18:00 slot
                        if (hour == endHour) {
                            Text(
                                text = "19",
                                modifier = Modifier
                                    .width(40.dp)
                                    .padding(end = 8.dp)
                                    .align(Alignment.BottomStart)
                                    .offset(y = 6.dp),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 40.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    .align(Alignment.BottomStart)
                            )
                        }
                    }
                }
            }

            // vertical Lines
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 40.dp)
            ) {
                days.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    )
                }
            }

            // lessons
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 40.dp)
            ) {
                days.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // filter and render lessons matching active weekday
                        val dayLessons = lessons.filter { it.dayOfWeek == day }

                        dayLessons.forEach { lesson ->
                            val startTime = try {
                                LocalTime.parse(lesson.startTime)
                            } catch (_: Exception) {
                                null
                            }
                            val endTime = try {
                                LocalTime.parse(lesson.endTime)
                            } catch (_: Exception) {
                                null
                            }

                            if (startTime != null && endTime != null) {
                                val minutesFromStart = ChronoUnit.MINUTES.between(
                                    LocalTime.of(startHour, 0),
                                    startTime
                                )
                                val durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime)

                                // offset and height calculations based on start and duration minutes
                                val topOffset = (minutesFromStart / 60f * hourHeight.value).dp
                                val height = (durationMinutes / 60f * hourHeight.value).dp

                                LessonGridItem(
                                    lesson = lesson,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .offset(y = topOffset)
                                        .height(height)
                                        .fillMaxWidth(),
                                    onClick = { onLessonClick(lesson) }
                                )
                            }
                        }
                    }
                }
            }

            // current time indicator (red line)
            currentTime?.let { time ->
                if (time.hour in startHour..endHour || (time.hour == 19 && time.minute == 0)) {
                    // current time red tracker line position calculations
                    val minutesFromStart = ChronoUnit.MINUTES.between(
                        LocalTime.of(startHour, 0),
                        time
                    )
                    val topOffset = (minutesFromStart / 60f * hourHeight.value).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 35.dp)
                            .offset(y = topOffset)
                            .height(2.dp)
                            .background(Color.Red)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.Red, CircleShape)
                                .align(Alignment.CenterStart)
                        )
                    }
                }
            }

            // show full-screen empty state if zero lessons are selected
            if (lessons.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.CalendarToday,
                    title = "Keine Vorlesungen geplant.",
                    description = "Für diese Woche sind keine Vorlesungen geplant oder ausgewählt.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * Einzelnes Kachel-Element innerhalb des Stundenplanrasters. Farblich markiert
 * nach Fakultätszugehörigkeit.
 *
 * @param lesson Die darzustellende Vorlesung.
 * @param modifier Der Modifier für Layout-Anpassungen des Kachel-Elements.
 * @param onClick Callback bei Klick auf die Kachel.
 */
@Composable
fun LessonGridItem(
    lesson: JsonLesson,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // determine color code matching course faculty prefixes
    val facultyColor = when {
        lesson.title.startsWith("eti-") -> Color(0xff00bfff)
        lesson.title.startsWith("mb-") -> Color(0xffffd700)
        lesson.title.startsWith("ws-") -> Color(0xff10b981)
        else -> MaterialTheme.colorScheme.primary
    }

    val displayTitle = remember(lesson.title) {
        lesson.title.substringAfter("-")
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = facultyColor.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, facultyColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(facultyColor)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // upper half: centered large course icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = remember(lesson.title) {
                        CourseIcons.getIcon(MockLogic.moduleEmojis[lesson.title])
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = facultyColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // lower half: course name and bigger, bolder location
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 10.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = lesson.room.substringBefore(" ("),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 11.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
