package com.example.timetable.view.components.timetable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Hilfsobjekt zum Auflösen von Icon-Namen in Material Design Vector-Icons.
 * Dient als zentrale Mapping-Stelle für Modul-Personalisierungen.
 */
object CourseIcons {
    val iconsMap = mapOf(
        "School" to Icons.Default.School,
        "Computer" to Icons.Default.Computer,
        "Code" to Icons.Default.Code,
        "Terminal" to Icons.Default.Terminal,
        "Memory" to Icons.Default.Memory,
        "Security" to Icons.Default.Security,
        "Calculate" to Icons.Default.Calculate,
        "Functions" to Icons.Default.Functions,
        "Science" to Icons.Default.Science,
        "Biotech" to Icons.Default.Biotech,
        "Psychology" to Icons.Default.Psychology,
        "MenuBook" to Icons.Default.MenuBook,
        "AutoStories" to Icons.Default.AutoStories,
        "Translate" to Icons.Default.Translate,
        "Architecture" to Icons.Default.Architecture,
        "Engineering" to Icons.Default.Engineering,
        "PrecisionManufacturing" to Icons.Default.PrecisionManufacturing,
        "ElectricBolt" to Icons.Default.ElectricBolt,
        "BusinessCenter" to Icons.Default.BusinessCenter,
        "AutoGraph" to Icons.Default.AutoGraph,
        "AccountBalance" to Icons.Default.AccountBalance,
        "Analytics" to Icons.Default.Analytics,
        "Language" to Icons.Default.Language,
        "Brush" to Icons.Default.Brush,
        "DesignServices" to Icons.Default.DesignServices,
        "Lightbulb" to Icons.Default.Lightbulb,
        "Eco" to Icons.Default.Eco,
        "RocketLaunch" to Icons.Default.RocketLaunch,
        "LocalHospital" to Icons.Default.LocalHospital,
        "MusicNote" to Icons.Default.MusicNote,
        "FitnessCenter" to Icons.Default.FitnessCenter
    )

    /**
     * Gibt das entsprechende ImageVector-Icon für einen Namen zurück.
     * Fallback ist das MenuBook-Icon.
     */
    fun getIcon(name: String?): ImageVector {
        return iconsMap[name] ?: Icons.Default.MenuBook
    }
}

/**
 * Horizontaler Icon-Selektor zur Personalisierung von Modulen.
 * Ermöglicht dem Nutzer, visuelle Merkmale für seine Vorlesungen festzulegen.
 *
 * @param selectedEmoji Der Bezeichner des aktuell gewählten Icons.
 * @param onEmojiSelected Callback bei der Auswahl eines neuen Icons.
 */
@Composable
fun EmojiSelector(
    selectedEmoji: String?,
    onEmojiSelected: (String) -> Unit
) {
    val iconsList = CourseIcons.iconsMap.keys.toList()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Kurs-Icon wählen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 24.dp)
        ) {
            items(iconsList) { iconName ->
                val icon = CourseIcons.getIcon(iconName)
                val isSelected = selectedEmoji == iconName

                // bouncy scale pop for the active selection
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "iconScale"
                )

                // smooth color transitions for background, border and tint
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    label = "iconBg"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "iconBorder"
                )

                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.8f
                    ),
                    label = "iconTint"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = CircleShape
                        )
                        .clickable { onEmojiSelected(iconName) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = iconName,
                        modifier = Modifier.size(20.dp),
                        tint = tintColor
                    )
                }
            }
        }
    }
}
