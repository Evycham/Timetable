package com.example.timetable.view.json

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * A temporary mock "ViewModel" bridge to simulate the DataStore/Logic Layer
 * for manual testing of UI features.
 */
object MockLogic {
    // Global active faculty color for the background waves
    var activeFacultyColor: Color? by mutableStateOf(null)

    // List of module titles currently in the user's plan
    val selectedModuleTitles = mutableStateListOf<String>(
        "eti-Autonome Mobile Systeme Vorl.",
        "mb-Mathematik II"
    )

    // Map of module titles to their chosen emojis
    val moduleEmojis = mutableStateMapOf<String, String>()

    fun addModule(title: String) {
        if (!selectedModuleTitles.contains(title)) {
            selectedModuleTitles.add(title)
        }
    }

    fun removeModule(title: String) {
        selectedModuleTitles.remove(title)
    }

    fun setEmoji(title: String, emoji: String) {
        moduleEmojis[title] = emoji
    }
}
