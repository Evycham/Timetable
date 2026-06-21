package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.model.CalenderDay
import com.example.timetable.data.repository.RepositorySyncState
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimetableViewModel(
    private val repository: TimetableRepository,
    private val userService: UserTimetableService
) : ViewModel() {

    val userCalenderDays: StateFlow<List<CalenderDay>> =
        userService.userCalenderDaysFlow()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val preferences: StateFlow<com.example.timetable.data.local.preferences.UserSchedulePreferences> =
        userService.preferencesFlow
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                com.example.timetable.data.local.preferences.UserSchedulePreferences()
            )

    val syncState: StateFlow<RepositorySyncState> = repository.syncState

    init {
        viewModelScope.launch {
            try {
                repository.initialize()
                repository.updateJsonIfNeeded()
            } catch (_: Exception) {
                // syncState reports repository errors
            }
        }
    }

    /**
     * Speichert oder aktualisiert ein Emoji für eine Veranstaltung
     */
    fun updateModuleEmoji(title: String, emoji: String) {
        viewModelScope.launch {
            userService.updateModuleEmoji(title, emoji)
        }
    }

    fun removeExtraModule(groupsCode: String, title: String) {
        viewModelScope.launch {
            userService.removeExtraLesson(groupsCode, title)
        }
    }

    fun hideModule(groupsCode: String, title: String) {
        viewModelScope.launch {
            userService.hideLesson(groupsCode, title)
        }
    }

    fun triggerRefresh() {
        viewModelScope.launch {
            try {
                repository.reloadJson()
            } catch (_: Exception) {
                // syncState reports repository errors
            }
        }
    }
}
