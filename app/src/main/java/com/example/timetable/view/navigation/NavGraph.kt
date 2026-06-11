package com.example.timetable.view.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.timetable.view.NavScreen
import com.example.timetable.view.screens.CourseSelectionScreen
import com.example.timetable.view.screens.InitialSetupScreen
import com.example.timetable.view.screens.SettingsScreen
import com.example.timetable.view.screens.TimetableScreen

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
fun TimetableNavHost() {
    // TODO [viewmodel]: Inject NavigationViewModel here to determine startDestination (Setup vs. Timetable)
    // navigation controller manages backstack and screen transitions
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route // TODO [viewmodel]: Use ViewModel state for dynamic startDestination
    ) {
        composable(Screen.Home.route) {
            NavScreen(navController = navController)
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
                    // reset backstack to home and navigate to setup when profile changes
                    navController.navigate(Screen.InitialSetup.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
    }
}
