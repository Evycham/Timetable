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
import com.example.timetable.view.screens.TimetableScreen

// alle möglichen Routen
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object InitialSetup : Screen("initial_setup")
    object Timetable : Screen("timetable/{course}") {
        fun createRoute(course: String) = "timetable/$course"
    }
    object CourseSelection : Screen("course_selection")
}

@Composable
fun TimetableNavHost() {
    // TODO [Logic]: Inject NavigationViewModel here to determine startDestination (Setup vs. Timetable)
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route // TODO [Logic]: Use ViewModel state for dynamic startDestination
    ) {
        composable(Screen.Home.route) {
            NavScreen(navController = navController)
        }
        composable(Screen.InitialSetup.route) {
            // TODO [Logic]: Provide SetupViewModel to InitialSetupScreen
            InitialSetupScreen(
                onNavigateToTimetable = { course ->
                    navController.navigate(Screen.Timetable.createRoute(course)) {
                        // doppelte Einträge im Backstack verhindern
                        launchSingleTop = true
                        // initialsetupScreen aus dem Stack entfernen
                        popUpTo(Screen.InitialSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Timetable.route,
            arguments = listOf(navArgument("course") { type = NavType.StringType })
        ) { backStackEntry ->
            val course = backStackEntry.arguments?.getString("course") ?: ""
            TimetableScreen(
                courseName = course,
                // zurücknavigieren erlaubt
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CourseSelection.route) {
            CourseSelectionScreen(
                // zurück navigieren erlaubt
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}