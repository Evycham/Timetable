package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService

class ViewModelFactory(
    private val repository: TimetableRepository,
    private val userService: UserTimetableService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppNavigationViewModel::class.java) ->
                AppNavigationViewModel(userService) as T

            modelClass.isAssignableFrom(InitialSetupViewModel::class.java) ->
                InitialSetupViewModel(repository, userService) as T

            modelClass.isAssignableFrom(TimetableViewModel::class.java) ->
                TimetableViewModel(repository, userService) as T

            modelClass.isAssignableFrom(CourseSelectionViewModel::class.java) ->
                CourseSelectionViewModel(repository, userService) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(userService) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
