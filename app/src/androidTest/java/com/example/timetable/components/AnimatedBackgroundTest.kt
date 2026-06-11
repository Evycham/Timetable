package com.example.timetable.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.timetable.view.components.common.AnimatedBackground
import com.example.timetable.view.theme.TimeTableTheme
import org.junit.Rule
import org.junit.Test

class AnimatedBackgroundTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun animatedBackground_rendersWithoutCrashing() {
        composeTestRule.setContent {
            TimeTableTheme {
                AnimatedBackground(accentColor = Color.Blue)
            }
        }
        
        // Assert compose framework executed the drawing without throwing errors
        composeTestRule.waitForIdle()
    }
}
