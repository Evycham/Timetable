package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.local.preferences.UserSchedulePreferences
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userService: UserTimetableService
) : ViewModel() {

    val preferences: StateFlow<UserSchedulePreferences> =
        userService.preferencesFlow
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                UserSchedulePreferences()
            )

    /**
     * Aktualisiert die Einstellung für das dynamische Farbschema
     */
    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userService.setDynamicColorEnabled(enabled)
        }
    }

    /**
     * Aktualisiert die Einstellung für Benachrichtigungen bei Vorlesungsausfällen
     */
    fun updateCancellationAlert(enabled: Boolean) {
        viewModelScope.launch {
            userService.setCancellationAlertEnabled(enabled)
        }
    }

    /**
     * Aktualisiert die Einstellung für Benachrichtigungen bei Raumänderungen
     */
    fun updateRoomChangeAlert(enabled: Boolean) {
        viewModelScope.launch {
            userService.setRoomChangeAlertEnabled(enabled)
        }
    }

    fun resetApp(onComplete: () -> Unit) {
        viewModelScope.launch {
            userService.clearPreferences()
            onComplete()
        }
    }
}
