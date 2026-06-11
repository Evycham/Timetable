package com.example.timetable.view.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetable.view.json.JsonLesson

/**
 * Karte im zur Anzeige von Suchergebnissen für Module.
 * Zeigt Moduldetails wie Dozent und Raum an und visualisiert Konflikte.
 *
 * @param lesson Die Daten der Vorlesung.
 * @param conflictInfo Informationen zu Zeitkonflikten (optional).
 * @param isSelected Gibt an, ob diese Karte aktuell ausgewählt ist.
 * @param onClick Callback, der bei Klick auf die Karte ausgeführt wird.
 */
@Composable
fun CompactGlassCard(
    lesson: JsonLesson,
    conflictInfo: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // bouncy scale animation to provide feedback for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    // smooth color transitions for selection states
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        label = "bgColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                borderColor,
                RoundedCornerShape(20.dp)
            ),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // main module title heading
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                // course meta information row
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Dozent",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " ${lesson.lecturer}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Ort",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " ${lesson.room.substringBefore(" (")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // highlight if this module overlaps with existing ones
                if (conflictInfo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = conflictInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // small color indicator for faculty recognition
            val facultyColor = when {
                lesson.title.startsWith("eti") -> Color(0xff00bfff)
                lesson.title.startsWith("mb") -> Color(0xffffd700)
                else -> MaterialTheme.colorScheme.secondary
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(facultyColor, RoundedCornerShape(4.dp))
            )
        }
    }
}
