package com.example.timetable.view.components.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.data.model.Event

/**
 * UX item das Kurse und Ferien (vorlesungsfreie Zeit) rendert
 */
@Composable
fun TimeTableEventItem(
    event: Event,
    modifier: Modifier = Modifier
) {
    val gradientColors = remember(event.category) {
        when (event.category?.lowercase()) {
            "Vorlesungsfreie Zeit", "holiday" -> listOf(
                Color(0xFFFF9A9E), // Soft pink
                Color(0xFFFECFEF)  // Soft pink-purple
            )

            "Feiertag", "public holiday" -> listOf(
                Color(0xFFA1C4FD), // Soft blue
                Color(0xFFC2E9FB)  // Pastel teal
            )

            else -> listOf(
                Color(0xFF84FAB0), // Mint green
                Color(0xFF8FD3F4)  // Pastel sky blue
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // gradient bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(Brush.verticalGradient(gradientColors))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // category icon wrapper
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.25f) })),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = if (event.category?.lowercase() in listOf(
                                "ferien",
                                "feiertag",
                                "holiday"
                            )
                        ) {
                            Icons.Default.Celebration
                        } else {
                            Icons.Default.Info
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = gradientColors.first().copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!event.category.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = event.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}
