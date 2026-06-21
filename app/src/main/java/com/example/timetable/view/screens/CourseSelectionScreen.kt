package com.example.timetable.view.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetable.view.components.selection.CompactGlassCard
import com.example.timetable.view.components.common.CompactGlassCardSkeleton
import com.example.timetable.view.components.selection.ConflictPreviewPanel
import com.example.timetable.view.components.common.EmptyStateView
import com.example.timetable.data.model.Lesson
import com.example.timetable.viewmodel.CourseSelectionViewModel

/**
 * Ein Bildschirm, der dem Benutzer die Suche nach Modulen ermöglicht.
 * Die Suche bietet eine Filterung nach Name, Dozent oder Raum.
 * Bei der Auswahl eines Moduls wird eine Konfliktprüfung durchgeführt, um eventuelle
 * Terminüberschneidungen mit bereits hinzugefügten Vorlesungen anzuzeigen, bevor
 * das Modul in den Plan eingetragen werden kann.
 *
 * @param viewModel Das ViewModel zur Modulsuche und -verwaltung.
 * @param onNavigateBack Callback-Methode zur Rückkehr zur vorherigen Ansicht.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSelectionScreen(
    viewModel: CourseSelectionViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val userLessons by viewModel.userLessons.collectAsState()
    val allLessons by viewModel.allLessons.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedResult by remember { mutableStateOf<Lesson?>(null) }

    // for hiding the keyboard on selection
    val keyboardController = LocalSoftwareKeyboardController.current

    val results = searchResults
    val isLoading = false

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Text(
                    text = "Modul wählen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }

            // text field fuzzy query filter input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchModules(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Name, Dozent, Raum...", modifier = Modifier.alpha(0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // dynamic content area switching between empty, loading, and result states
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.length < 2) {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "Modul suchen",
                        description = "Gib den Namen der Vorlesung, den Dozenten oder den Raum ein, um deinen Stundenplan zu filtern."
                    )
                } else if (isLoading) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(5) {
                            CompactGlassCardSkeleton()
                        }
                    }
                } else if (results.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = "Keine Ergebnisse",
                        description = "Für \"$searchQuery\" wurden keine passenden Module gefunden. Versuche es mit einem anderen Namen oder Raum."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(results) { index, lesson ->
                            val moduleLessons = allLessons.filter { it.title == lesson.title }
                            val conflict = findFirstConflict(moduleLessons, userLessons)

                            var isVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { isVisible = true }

                            Box {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 400,
                                            delayMillis = index * 50
                                        )
                                    ) +
                                            slideInVertically(
                                                animationSpec = tween(
                                                    durationMillis = 400,
                                                    delayMillis = index * 50
                                                ),
                                                initialOffsetY = { 20 }
                                            )
                                ) {
                                    CompactGlassCard(
                                        lesson = lesson,
                                        conflictInfo = conflict,
                                        isSelected = selectedResult?.title == lesson.title,
                                        onClick = {
                                            if (selectedResult?.title == lesson.title) {
                                                selectedResult = null
                                            } else {
                                                keyboardController?.hide()
                                                selectedResult = lesson
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // action overlay sheet requesting add confirmation
        AnimatedVisibility(
            visible = selectedResult != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedResult?.let { lesson ->
                val moduleLessons = allLessons.filter { it.title == lesson.title }
                val conflict = findFirstConflict(moduleLessons, userLessons)

                val primaryCode = preferences.groupsCode ?: ""
                val isCoreModule = lesson.groupsCode.contains(primaryCode)
                val isCurrentlyVisible = userLessons.any { it.title == lesson.title }

                val buttonText = if (isCoreModule) {
                    if (isCurrentlyVisible) "Ausblenden" else "Einblenden"
                } else {
                    if (isCurrentlyVisible) "Aus Plan entfernen" else "In den Plan aufnehmen"
                }

                ConflictPreviewPanel(
                    lesson = lesson,
                    conflictInfo = conflict,
                    buttonText = buttonText,
                    onAdd = {
                        if (isCoreModule) {
                            if (isCurrentlyVisible) {
                                viewModel.hideModule(primaryCode, lesson.title)
                            } else {
                                viewModel.showModule(primaryCode, lesson.title)
                            }
                        } else {
                            if (isCurrentlyVisible) {
                                viewModel.removeExtraModule(primaryCode, lesson.title)
                            } else {
                                viewModel.addExtraModule(primaryCode, lesson.title)
                            }
                        }
                        onNavigateBack()
                    }
                )
            }
        }
    }
}

/**
 * Durchsucht die Liste der bereits eingetragenen Vorlesungen nach einem Termin-Konflikt
 * mit einem neu hinzuzufügenden Modul.
 *
 * @param newModuleLessons Liste der Vorlesungen des neuen Moduls.
 * @param existingLessons Liste der bereits im Plan vorhandenen Vorlesungen.
 * @return Eine Fehlermeldung mit Tag, Uhrzeit und Konflikt-Modul, wenn ein Konflikt existiert, andernfalls `null`.
 */
private fun findFirstConflict(
    newModuleLessons: List<Lesson>,
    existingLessons: List<Lesson>
): String? {
    // helper method resolving first matching slot conflict
    for (newLesson in newModuleLessons) {
        for (existing in existingLessons) {
            if (newLesson.date == existing.date &&
                newLesson.startTime == existing.startTime
            ) {
                val day = try {
                    java.time.LocalDate.parse(newLesson.date).dayOfWeek.name.take(2)
                } catch (_: Exception) {
                    ""
                }
                return "$day ${newLesson.startTime} - Kollision mit ${existing.title}"
            }
        }
    }
    return null
}
