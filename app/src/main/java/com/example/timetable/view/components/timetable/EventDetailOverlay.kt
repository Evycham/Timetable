package com.example.timetable.view.components.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.data.model.Lesson

/**
 * Interaktives Overlay zur Anzeige von detaillierten Informationen zu einer Vorlesung.
 * Das Overlay bietet Funktionen zum Anpassen des Modul-Icons und zum Entfernen des Moduls aus dem Plan.
 *
 * @param lesson Das ausgewählte Vorlesungsobjekt oder null, wenn kein Overlay angezeigt werden soll.
 * @param customEmoji Das aktuell zugeordnete Emoji.
 * @param bottomPadding Zusätzlicher Abstand am unteren Rand (z. B. für die NavigationBar).
 * @param onEmojiSelected Callback, wenn ein neues Emoji ausgewählt wird.
 * @param onRemoveClick Callback, wenn der Kurs aus dem Plan gelöscht werden soll.
 * @param onDismiss Callback-Funktion zum Schließen des Overlays.
 */
@Composable
fun EventDetailOverlay(
    lesson: Lesson?,
    customEmoji: String? = null,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onEmojiSelected: (String) -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    // reactive fetch of current emoji state from parameters
    val selectedEmoji = customEmoji
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // reset the confirmation dialog if the underlying lesson changes or becomes null
    LaunchedEffect(lesson) {
        if (lesson == null) {
            showDeleteConfirmation = false
        }
    }

    // semi-transparent backdrop with blur to focus user attention on the card
    AnimatedVisibility(
        visible = lesson != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .blur(8.dp)
                .clickable { onDismiss() } // close when clicking outside
        )
    }

    // information card that slides up from the bottom
    AnimatedVisibility(
        visible = lesson != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 12.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 24.dp,
                            bottom = 24.dp + bottomPadding
                        )
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()) // allow scrolling for long change messages or small screens
                ) {
                    // overlay header with title label and close action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Vorlesungsdetails",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    lesson?.let { l ->
                        // course title section
                        Text(
                            text = l.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                lineHeight = 36.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // component to allow personalization via icon selection
                        EmojiSelector(
                            selectedEmoji = selectedEmoji,
                            onEmojiSelected = onEmojiSelected
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // core information rows (time, location, teacher)
                        DetailInfoRow(
                            icon = Icons.Default.Schedule,
                            label = "Zeitraum",
                            value = "${l.startTime} - ${l.endTime}"
                        )

                        val roomValue = l.rooms?.joinToString(", ") ?: "Raum noch nicht bekannt"
                        DetailInfoRow(
                            icon = Icons.Default.LocationOn,
                            label = "Ort",
                            value = "$roomValue (${l.building ?: "-"})"
                        )

                        val lecturerValue =
                            l.teacher?.joinToString(", ") ?: "Dozent wurde noch nicht hinterlegt"
                        DetailInfoRow(
                            icon = Icons.Default.Person,
                            label = "Dozent / Dozentin",
                            value = lecturerValue
                        )

                        // special section for cancellations or room changes
                        l.change?.let { change ->
                            val isCancellation = change.reasonType == "cancellation"
                            val containerColor = if (isCancellation) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                            }
                            val contentColor = if (isCancellation) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            }
                            val borderColor = if (isCancellation) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = containerColor,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (isCancellation) Icons.Default.Cancel else Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = change.caption ?: "Wichtige Änderung",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    val messageDetail = change.modified
                                    if (!messageDetail.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = messageDetail,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // primary action to remove module from user's personal plan
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aus Plan entfernen",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // safety dialog to prevent accidental removal
    if (showDeleteConfirmation && lesson != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Kurs entfernen?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Möchtest du den Kurs \"${lesson.title.substringAfter("-")}\" wirklich aus deinem Stundenplan löschen?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onRemoveClick()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Entfernen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Abbrechen")
                }
            },
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        )
    }
}
