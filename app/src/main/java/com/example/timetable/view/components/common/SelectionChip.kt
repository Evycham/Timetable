package com.example.timetable.view.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interaktiver Chip zur Auswahl von Optionen (z. B. Fakultäten oder Studiengänge).
 * Bietet visuelles Feedback durch Animationen von Skalierung, Hintergrund- und Rahmenfarbe.
 *
 * @param label Der anzuzeigende Text auf dem Chip.
 * @param color Die Akzentfarbe, die bei Auswahl verwendet wird.
 * @param isSelected Gibt an, ob der Chip aktuell ausgewählt ist.
 * @param testTagPrefix Präfix für Test-Tags zur Identifizierung im UI-Test.
 * @param onClick Callback, der bei Klick auf den Chip ausgeführt wird.
 */
@Composable
fun SelectionChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    testTagPrefix: String,
    onClick: () -> Unit
) {
    // subtle scale pop when selected for better visual depth
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chipScale"
    )

    // animated color transitions for a fluid feel
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else Color.White.copy(alpha = 0.1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chipBorder"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chipBackground"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isSelected) Modifier.testTag("${testTagPrefix}_${label}") else Modifier)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
        color = backgroundColor,
        tonalElevation = if (isSelected) 8.dp else 0.dp // elevate slightly when picked
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    letterSpacing = if (isSelected) 0.5.sp else 0.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
