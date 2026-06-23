package com.example.timetable.view.navigation

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationTest {

    private fun setupNavControllerWithPathRoute(): NavController {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        
        val graph = navController.createGraph(startDestination = "home") {
            composable("home") {}
            composable(
                route = "timetable/{course}",
                arguments = listOf(navArgument("course") { type = NavType.StringType })
            ) {}
        }
        navController.graph = graph
        return navController
    }

    private fun setupNavControllerWithQueryRoute(): NavController {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        
        val graph = navController.createGraph(startDestination = "home") {
            composable("home") {}
            composable(
                route = "timetable?course={course}",
                arguments = listOf(
                    navArgument("course") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {}
        }
        navController.graph = graph
        return navController
    }

    @Test
    fun testTheory1_pathWithRawSlashesFails() {
        val navController = setupNavControllerWithPathRoute()
        
        // Assert that raw slashes in path parameter cause IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            navController.navigate("timetable/ws-1./2./3. GOEK-M")
        }
    }

    @Test
    fun testTheory2_pathWithUriEncodedSlashes() {
        val navController = setupNavControllerWithPathRoute()
        val course = "ws-1./2./3. GOEK-M"
        val encodedCourse = android.net.Uri.encode(course) // "ws-1.%2F2.%2F3.%20GOEK-M"
        
        // Let's see if navigating with encoded path works or fails
        try {
            navController.navigate("timetable/$encodedCourse")
            val arg = navController.currentBackStackEntry?.arguments?.getString("course")
            println("Theory 2: URI encoded path navigation succeeded. Received arg: $arg")
        } catch (e: IllegalArgumentException) {
            println("Theory 2: URI encoded path navigation failed: ${e.message}")
            throw e
        }
    }

    @Test
    fun testTheory3_pathWithSanitizedSlashesSucceeds() {
        val navController = setupNavControllerWithPathRoute()
        val course = "ws-1./2./3. GOEK-M"
        // Replace / with -
        val sanitizedCourse = course.replace("/", "-")
        
        navController.navigate("timetable/$sanitizedCourse")
        
        assertEquals("timetable/{course}", navController.currentBackStackEntry?.destination?.route)
        assertEquals("ws-1.-2.-3. GOEK-M", navController.currentBackStackEntry?.arguments?.getString("course"))
    }

    @Test
    fun testTheory4_queryWithRawAndEncodedSlashes() {
        val navController = setupNavControllerWithQueryRoute()
        val course = "ws-1./2./3. GOEK-M"
        
        // 4a. Raw query parameter (should succeed because path matching is done on the base path)
        try {
            navController.navigate("timetable?course=$course")
            val arg = navController.currentBackStackEntry?.arguments?.getString("course")
            println("Theory 4a: Raw query navigation succeeded. Received arg: $arg")
            assertEquals(course, arg)
        } catch (e: Exception) {
            println("Theory 4a: Raw query navigation failed: ${e.message}")
            throw e
        }

        // 4b. Encoded query parameter (best practice for query params)
        val encodedCourse = android.net.Uri.encode(course)
        navController.navigate("timetable?course=$encodedCourse")
        assertEquals("timetable?course={course}", navController.currentBackStackEntry?.destination?.route)
        assertEquals(course, navController.currentBackStackEntry?.arguments?.getString("course"))
    }

    @Test
    fun testUriEncodingAndDecodingWithFacultyPrefix() {
        val course = "ws-1./2./3. GOEK-M"
        
        // 1. Encoding (similar to Screen.Timetable.createRoute)
        val route = "timetable/${android.net.Uri.encode(course)}"
        
        // 2. Decoding (similar to AnimatedBackground)
        val extractedCourse = route.substringAfter("timetable/")
            .let { android.net.Uri.decode(it) }
        
        assertEquals(course, extractedCourse)
        
        // 3. Faculty prefix matching
        val matched = com.example.timetable.utils.enums.Faculty.entries.find { faculty ->
            extractedCourse.startsWith(faculty.prefix, ignoreCase = true)
        }
        assertEquals(com.example.timetable.utils.enums.Faculty.WS, matched)
    }
}
