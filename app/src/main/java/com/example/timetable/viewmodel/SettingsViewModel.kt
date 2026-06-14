package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import com.example.timetable.data.services.UserTimetableService

class SettingsViewModel(
    private val userService: UserTimetableService
) : ViewModel()
