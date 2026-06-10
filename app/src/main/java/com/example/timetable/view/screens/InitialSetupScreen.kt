package com.example.timetable.view.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.utils.enums.Faculty
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.platform.LocalContext
import com.example.timetable.view.json.JsonLessonRepository

@Composable
fun InitialSetupScreen(
    // TODO [Logic]: Accept SetupViewModel or SetupUiState here
    onNavigateToTimetable: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { JsonLessonRepository(context) }

    // TODO [Logic]: Replace these local states with state collected from the ViewModel
    var searchQuery by remember { mutableStateOf("") }
    var selectedFaculty by remember { mutableStateOf<Faculty?>(null) }
    var selectedCourse by remember { mutableStateOf<String?>(null) }

    // Animation für die "Wellenbewegung" (UX Agent: Organische Hintergrund-Animation)
    val infiniteTransition = rememberInfiniteTransition(label = "BgMovement")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // hintergrund-glow (UX Agent: Mehrlagige radiale Gradienten für Tiefe und Bewegung)
        val baseColor = selectedFaculty?.color ?: MaterialTheme.colorScheme.primary

        val glowColor1 by animateColorAsState(
            targetValue = baseColor.copy(alpha = 0.18f),
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
            label = "Glow1"
        )
        val glowColor2 by animateColorAsState(
            targetValue = baseColor.copy(alpha = 0.12f),
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
            label = "Glow2"
        )
        val glowColor3 by animateColorAsState(
            targetValue = baseColor.copy(alpha = 0.10f),
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
            label = "Glow3"
        )

        // Blob 1: Rechts Oben (Haupt-Welle)
        Box(
            modifier = Modifier
                .offset {
                    val x = (50.dp + (60.dp * sin(phase.toDouble()).toFloat()))
                    val y = ((-100).dp + (30.dp * cos(phase.toDouble()).toFloat()))
                    with(density) { IntOffset(x.roundToPx(), y.roundToPx()) }
                }
                .size(600.dp)
                .background(Brush.radialGradient(listOf(glowColor1, Color.Transparent)))
        )

        // Blob 2: Links Mitte (Gegen-Welle)
        Box(
            modifier = Modifier
                .offset {
                    val x = ((-150).dp + (80.dp * cos(phase.toDouble() * 0.7).toFloat()))
                    val y = (200.dp + (40.dp * sin(phase.toDouble() * 0.5).toFloat()))
                    with(density) { IntOffset(x.roundToPx(), y.roundToPx()) }
                }
                .size(500.dp)
                .background(Brush.radialGradient(listOf(glowColor2, Color.Transparent)))
        )

        // Blob 3: Unten Mitte (Sanfter Aufgang)
        Box(
            modifier = Modifier
                .offset {
                    val x = (0.dp + (100.dp * sin(phase.toDouble() * 0.4).toFloat()))
                    val y = (400.dp + (20.dp * cos(phase.toDouble() * 0.8).toFloat()))
                    with(density) { IntOffset(x.roundToPx(), y.roundToPx()) }
                }
                .size(550.dp)
                .background(Brush.radialGradient(listOf(glowColor3, Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // typografie
            Text(
                text = "HOSTvinci",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    lineHeight = 52.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Dein übersichtlicher Stundenplan für die HOST.",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = selectedFaculty,
                label = "SetupStateTransition",
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState != null) it else -it },
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ) + fadeIn() togetherWith slideOutHorizontally(
                        targetOffsetX = { if (targetState != null) -it else it },
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ) + fadeOut()
                }
            ) { faculty ->
                if (faculty == null) {
                    // --- STATE 1: Fakultät auswählen ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Fakultät auswählen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Vertikale Fakultätsliste
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // TODO [Logic]: Use faculty list from ViewModel state (uiState.faculties)
                            Faculty.entries.forEach { f ->
                                SelectionChip(
                                    label = f.label,
                                    color = f.color,
                                    isSelected = false,
                                    testTagPrefix = "selectedFacultyChip",
                                    onClick = {
                                        selectedFaculty = f
                                    } // TODO [Logic]: Call viewModel.onFacultySelected(f)
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                } else {
                    // --- STATE 2: Studiengang auswählen ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Zurück-Button & Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    selectedFaculty = null
                                    selectedCourse = null
                                    searchQuery = ""
                                },
                                modifier = Modifier.offset(x = (-12).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Zurück zur Fakultätsauswahl",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = faculty.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Studiengänge & Suche
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("courseSearchField"),
                                placeholder = {
                                    Text("Studiengang suchen...", modifier = Modifier.alpha(0.5f))
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search Icon",
                                        tint = faculty.color
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.3f
                                    ),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    ),
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = faculty.color
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Studiengang auswählen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                            )

                            // TODO [Logic]: Use the filtered course list directly from the ViewModel state (uiState.filteredCourses)
                            val courses = remember(selectedFaculty, searchQuery) {
                                selectedFaculty?.let { faculty ->
                                    repository.getCoursesByFaculty(faculty.prefix)
                                        .filter { it.contains(searchQuery, ignoreCase = true) }
                                } ?: emptyList()
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                courses.forEach { course ->
                                    SelectionChip(
                                        label = course,
                                        color = faculty.color,
                                        isSelected = selectedCourse == course,
                                        testTagPrefix = "selectedCourseChip",
                                        onClick = {
                                            selectedCourse = course
                                        } // TODO [Logic]: Call viewModel.onCourseSelected(course)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                        }

                        // Action Button
                        val buttonTextColor =
                            if (faculty.color.luminance() > 0.5f) Color.Black else Color.White

                        Button(
                            onClick = {
                                // TODO [Logic]: Call viewModel.onCompleteSetup(selectedCourse)
                                selectedCourse?.let { onNavigateToTimetable(it) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .padding(bottom = 32.dp)
                                .testTag("setupContinueButton"),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = faculty.color,
                                contentColor = buttonTextColor
                            ),
                            elevation = ButtonDefaults.buttonColors().let {
                                ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
                            }
                        ) {
                            Text(
                                text = "Stundenplan anzeigen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    testTagPrefix: String,
    onClick: () -> Unit
) {
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
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
        color = backgroundColor,
        tonalElevation = if (isSelected) 8.dp else 0.dp
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
                // Fix für den Kontrast: Immer onSurface anstatt die Fakultätsfarbe
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
