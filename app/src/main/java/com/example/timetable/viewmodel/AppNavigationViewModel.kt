package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppNavigationViewModel(
    userService: UserTimetableService
) : ViewModel() {

    val isSetupComplete: StateFlow<Boolean?> =
        userService.preferencesFlow
            .map { preferences -> preferences.isSetupComplete }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentGroupsCode: StateFlow<String?> =
        userService.preferencesFlow
            .map { preferences -> preferences.groupsCode }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
