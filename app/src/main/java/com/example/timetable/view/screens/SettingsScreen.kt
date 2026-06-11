package com.example.timetable.view.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetable.utils.getVersionName
import com.example.timetable.view.json.MockLogic

/**
 * Das Einstellungsmenü der App. Ermöglicht die Anpassung von Benutzereinstellungen wie
 * dem aktiven Studiengang, Farb-Themes und Benachrichtigungseinstellungen.
 *
 * @param onNavigateBack Callback-Methode zur Rückkehr zur vorherigen Ansicht.
 * @param onNavigateToSetup Callback-Methode zum Zurücksetzen oder Wechseln der Fakultäts- und Kursauswahl.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSetup: () -> Unit = {}
) {
    // track theme and switch choices locally using state wrappers
    var isDynamicColorEnabled by remember { mutableStateOf(false) }
    var isCancellationAlertEnabled by remember { mutableStateOf(true) }
    var isRoomChangeAlertEnabled by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Einstellungen",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // profile study program management section
            SettingsSectionHeader(title = "Profil & Studiengang")

            // compute dynamic text label based on selections in MockLogic
            val activeModulesText = remember {
                derivedStateOf {
                    if (MockLogic.selectedModuleTitles.isEmpty()) "Keine Kurse ausgewählt"
                    else "${MockLogic.selectedModuleTitles.size} Kurse im Plan"
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dein Semesterwochenplan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = activeModulesText.value,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // visual action card for changing current active faculty/course
                    Button(
                        onClick = onNavigateToSetup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(text = "Studiengang wechseln", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // dynamic color scheme theme settings section
            SettingsSectionHeader(title = "Design & Themes")
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.ColorLens,
                        title = "Dynamische Farben",
                        subtitle = "Farben an dein System anpassen",
                        checked = isDynamicColorEnabled,
                        onCheckedChange = { isDynamicColorEnabled = it }
                    )
                }
            }

            // alert notifications configuration section
            SettingsSectionHeader(title = "Benachrichtigungen")
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        title = "Vorlesungsausfälle",
                        subtitle = "Benachrichtigung bei abgesagten Vorlesungen",
                        checked = isCancellationAlertEnabled,
                        onCheckedChange = { isCancellationAlertEnabled = it }
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        title = "Raumänderungen",
                        subtitle = "Benachrichtigung bei Raum- oder Zeitänderungen",
                        checked = isRoomChangeAlertEnabled,
                        onCheckedChange = { isRoomChangeAlertEnabled = it }
                    )
                }
            }

            // app static details and developer metadata
            SettingsSectionHeader(title = "Über HOSTvinci")
            val context = LocalContext.current
            val versionName = getVersionName(context)

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "HOSTvinci Semesterwochenplan App",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version ${versionName} - Hochschule Stralsund",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Eine einfache Überschriften-Zeile zur Strukturierung der einzelnen Einstellungsabschnitte.
 *
 * @param title Der anzuzeigende Titel des Abschnitts (z. B. "Benachrichtigungen").
 */
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

/**
 * Eine Tabellenzeile innerhalb eines Einstellungskartenbereichs, die einen Ein-/Ausschalter (Switch)
 * zusammen mit einem Symbol, einem Titel und einer Kurzbeschreibung enthält.
 *
 * @param icon Das Vektorsymbol, das auf der linken Seite angezeigt wird.
 * @param title Die Bezeichnung der Einstellung.
 * @param subtitle Die Beschreibung der Auswirkungen dieser Einstellung.
 * @param checked Ob der Schalter aktuell aktiv (ein) ist.
 * @param onCheckedChange Callback, wenn sich der Zustand des Schalters ändert.
 */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // switch container row wrapper responding to tap gestures
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
