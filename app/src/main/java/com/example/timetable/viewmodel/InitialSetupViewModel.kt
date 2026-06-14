package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import com.example.timetable.utils.enums.Faculty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InitialSetupUiState(
    val faculties: List<Faculty> = emptyList(),
    val selectedFaculty: Faculty? = null,
    val searchQuery: String = "",
    val filteredCourses: List<String> = emptyList(),
    val isSetupComplete: Boolean = false
)

class InitialSetupViewModel(
    private val repository: TimetableRepository,
    private val userService: UserTimetableService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InitialSetupUiState())
    val uiState: StateFlow<InitialSetupUiState> = _uiState

    init {
        loadFaculties()
    }

    fun selectFaculty(faculty: Faculty) {
        _uiState.value = _uiState.value.copy(
            selectedFaculty = faculty,
            searchQuery = "",
            filteredCourses = filterCourses(faculty, "")
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredCourses = filterCourses(_uiState.value.selectedFaculty, query)
        )
    }

    fun completeSetup(course: String) {
        viewModelScope.launch {
            userService.completeSetup(course)
            _uiState.value = _uiState.value.copy(isSetupComplete = true)
        }
    }

    private fun loadFaculties() {
        viewModelScope.launch {
            try {
                repository.initialize()
                _uiState.value = _uiState.value.copy(
                    faculties = getFacultiesFromLessons()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(faculties = emptyList())
            }
        }
    }

    private fun filterCourses(faculty: Faculty?, query: String): List<String> {
        if (faculty == null) return emptyList()

        val facultyPrefix = faculty.prefix.removeSuffix("-")
        return repository.getAllLessons()
            .flatMap { lesson -> lesson.groupsCode }
            .distinct()
            .filter { course -> course.startsWith(facultyPrefix, ignoreCase = true) }
            .filter { course -> course.contains(query, ignoreCase = true) }
            .sorted()
    }

    private fun getFacultiesFromLessons(): List<Faculty> {
        val courses = repository.getAllLessons()
            .flatMap { lesson -> lesson.groupsCode }

        return Faculty.entries.filter { faculty ->
            val facultyPrefix = faculty.prefix.removeSuffix("-")
            courses.any { course -> course.startsWith(facultyPrefix, ignoreCase = true) }
        }
    }
}
