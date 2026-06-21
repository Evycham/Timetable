package com.example.timetable.view.components.timetable

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.data.model.Lesson
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lesson Card.
 * "isPast" state and temporary pulse effect for changes.
 */
@Composable
fun TimetableLessonCard(
    lesson: Lesson,
    accentColor: Color,
    isPast: Boolean = false,
    customEmoji: String? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var showPulse by remember { mutableStateOf(lesson.change != null) }
    val isCancellation = remember(lesson.change) { lesson.isChangeCancellation }

    // pulse effect logic: stop after 10 seconds
    LaunchedEffect(lesson.change) {
        if (lesson.change != null) {
            showPulse = true
            delay(10000.milliseconds)
            showPulse = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val targetPulseColor = if (showPulse) {
        if (isCancellation) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }
    val pulseColor by infiniteTransition.animateColor(
        initialValue = Color.Transparent,
        targetValue = targetPulseColor,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "PressScale"
    )

    val alpha = if (isPast) 0.4f else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            if (showPulse) 2.dp else 1.dp,
            if (showPulse) pulseColor else Color.White.copy(alpha = 0.15f)
        ),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // accent stripe
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isPast) Color.Gray else accentColor,
                                (if (isPast) Color.Gray else accentColor).copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val icon = remember(lesson.title, customEmoji) {
                            CourseIcons.getIcon(customEmoji)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isPast) Color.Gray else accentColor,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp)
                        )

                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (isPast) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Erledigt",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }

                    if (lesson.change != null) {
                        val badgeColor =
                            if (isCancellation) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                        val badgeTextColor =
                            if (isCancellation) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        val badgeText = if (isCancellation) "Fällt aus" else "Änderung"

                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LessonInfoItem(
                        Icons.Default.Schedule,
                        "${lesson.startTime} - ${lesson.endTime}"
                    )

                    val roomText =
                        lesson.rooms?.firstOrNull()?.substringBefore(" (")?.ifBlank { "Raum n.a." }
                            ?: "Raum n.a."
                    LessonInfoItem(Icons.Default.LocationOn, roomText)
                }
            }
        }
    }
}

@Composable
private fun LessonInfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private val Lesson.isChangeCancellation: Boolean
    get() = change?.let {
        it.reasonType == "cancellation" ||
                it.caption?.contains("aus", ignoreCase = true) == true ||
                it.caption?.contains("cancell", ignoreCase = true) == true
    } ?: false
