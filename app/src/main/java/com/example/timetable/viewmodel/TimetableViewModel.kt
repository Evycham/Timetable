package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService

class TimetableViewModel(
    private val repository: TimetableRepository,
    private val userService: UserTimetableService
) : ViewModel()
