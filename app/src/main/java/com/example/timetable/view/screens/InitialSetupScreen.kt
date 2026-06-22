package com.example.timetable.view.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.example.timetable.view.components.common.SelectionChip
import com.example.timetable.viewmodel.InitialSetupViewModel
import com.example.timetable.view.theme.LocalBackgroundAccentColor

/**
 * Der Onboarding-Bildschirm für die Ersteinrichtung der App.
 * Führt den Nutzer durch die Auswahl seiner Fakultät und seines Studiengangs, um einen
 * initialen Stundenplan zu erstellen.
 *
 * @param viewModel Das ViewModel zur Verwaltung des Onboardings.
 * @param onNavigateToTimetable Callback zum Navigieren zur Hauptansicht nach erfolgreicher Auswahl.
 */
@Composable
fun InitialSetupScreen(
    viewModel: InitialSetupViewModel,
    onNavigateToTimetable: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCourse by remember { mutableStateOf<String?>(null) }

    val searchQuery = uiState.searchQuery
    val selectedFaculty = uiState.selectedFaculty

    val localAccentColor = LocalBackgroundAccentColor.current
    LaunchedEffect(selectedFaculty) {
        localAccentColor.value = selectedFaculty?.color
    }

    DisposableEffect(Unit) {
        onDispose {
            localAccentColor.value = null
        }
    }

    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) {
            selectedCourse?.let {
                onNavigateToTimetable(it)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // branding header with custom typography
            Text(
                text = "HOSTvinci",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    lineHeight = 52.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Text(
                text = "Dein übersichtlicher Stundenplan für die HOST.",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // dynamic content switching based on selection stage
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
                    // --- STAGE 1: Faculty Selection ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Fakultät auswählen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.faculties.forEach { f ->
                                SelectionChip(
                                    label = f.label,
                                    color = f.color,
                                    isSelected = false,
                                    testTagPrefix = "selectedFacultyChip",
                                    onClick = {
                                        viewModel.selectFaculty(f)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                } else {
                    // --- STAGE 2: Course Selection ---
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                        ) {
                            // navigation header to return to faculty choice
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.selectFaculty(null)
                                        selectedCourse = null
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

                            // search and list area for courses
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("courseSearchField"),
                                    placeholder = {
                                        Text(
                                            "Studiengang suchen...",
                                            modifier = Modifier.alpha(0.5f)
                                        )
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

                                val courses = uiState.filteredCourses

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
                                            }
                                        )
                                    }
                                }

                                // bottom spacer to ensure content isn't hidden by the action button
                                Spacer(modifier = Modifier.height(180.dp))
                            }
                        }

                        // bottom overlay with gradient and completion button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // adjust text color based on button color brightness
                            val buttonTextColor =
                                if (faculty.color.luminance() > 0.5f) Color.Black else Color.White

                            Button(
                                onClick = {
                                    selectedCourse?.let {
                                        viewModel.completeSetup(it)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                                    .height(64.dp)
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
}
