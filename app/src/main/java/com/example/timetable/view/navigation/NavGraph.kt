package com.example.timetable.view.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import com.example.timetable.view.screens.CourseSelectionScreen
import com.example.timetable.view.screens.InitialSetupScreen
import com.example.timetable.view.screens.SettingsScreen
import com.example.timetable.view.screens.TimetableScreen
import com.example.timetable.viewmodel.AppNavigationViewModel
import com.example.timetable.viewmodel.ViewModelFactory

/**
 * Repräsentiert die verschiedenen Navigationsziele (Bildschirme) innerhalb der Anwendung.
 * Jedes Ziel ist einer eindeutigen Route zugeordnet.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object InitialSetup : Screen("initial_setup")
    object Timetable : Screen("timetable/{course}") {
        fun createRoute(course: String) = "timetable/$course"
    }

    object CourseSelection : Screen("course_selection")
    object Settings : Screen("settings")
}

/**
 * Der Navigations-Host der App, der die Routen-Definitionen und die Navigation
 * zwischen den verschiedenen Ansichten (Home, Setup, Stundenplan, Kursauswahl, Einstellungen) verwaltet.
 */
@Composable
fun TimetableNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext
    val repository = remember { TimetableRepository(context) }
    val userService = remember {
        UserTimetableService(
            repository = repository,
            preferencesStore = UserSchedulePreferencesStore(context.userSchedulePreferencesDataStore),
            database = TimetableDatabase.getInstance(context)
        )
    }
    val factory = remember { ViewModelFactory(repository, userService) }
    val navigationViewModel: AppNavigationViewModel = viewModel(factory = factory)

    val isSetupComplete by navigationViewModel.isSetupComplete.collectAsState()
    val currentGroupsCode by navigationViewModel.currentGroupsCode.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            if (isSetupComplete != null) {
                LaunchedEffect(isSetupComplete, currentGroupsCode) {
                    val route = if (isSetupComplete == true && !currentGroupsCode.isNullOrBlank()) {
                        Screen.Timetable.createRoute(currentGroupsCode.orEmpty())
                    } else {
                        Screen.InitialSetup.route
                    }

                    navController.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize())
        }
        composable(Screen.InitialSetup.route) {
            // TODO [viewmodel]: Provide SetupViewModel to InitialSetupScreen
            // setup screen configuration callback
            InitialSetupScreen(
                onNavigateToTimetable = { course ->
                    navController.navigate(Screen.Timetable.createRoute(course)) {
                        // prevent double setup screens on backstack
                        launchSingleTop = true
                        // drop setup screen from stack after navigating
                        popUpTo(Screen.InitialSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Timetable.route,
            arguments = listOf(navArgument("course") { type = NavType.StringType })
        ) { backStackEntry ->
            // route definition for main weekly/daily schedule
            val course = backStackEntry.arguments?.getString("course") ?: ""
            TimetableScreen(
                courseName = course,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCourseSelection = { navController.navigate(Screen.CourseSelection.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.CourseSelection.route) {
            // course selection and search overlay screen
            CourseSelectionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            // settings navigation routing destination
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSetup = {
                    navController.navigate(Screen.InitialSetup.route) {
                        popUpTo(Screen.Timetable.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
