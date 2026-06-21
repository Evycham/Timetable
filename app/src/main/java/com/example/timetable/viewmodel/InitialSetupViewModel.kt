package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import com.example.timetable.utils.enums.Faculty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InitialSetupUiState(
    val faculties: List<Faculty> = emptyList(),
    val selectedFaculty: Faculty? = null,
    val searchQuery: String = "",
    val filteredCourses: List<String> = emptyList(),
    val isSetupComplete: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class InitialSetupViewModel(
    private val repository: TimetableRepository,
    private val userService: UserTimetableService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InitialSetupUiState())
    val uiState: StateFlow<InitialSetupUiState> = _uiState
    private var allCoursesCache: List<String> = emptyList()

    init {
        loadFaculties()
    }

    fun retryLoading() {
        loadFaculties()
    }

    private fun loadFaculties() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(isLoading = true, errorMessage = null)
            }

            try {
                repository.initialize()
                allCoursesCache = getCoursesFromLessons()
                _uiState.update { current ->
                    current.copy(
                        faculties = getFacultiesFromCourses(),
                        isLoading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        faculties = emptyList(),
                        isLoading = false,
                        errorMessage = "Fehler beim Laden der Studiengänge. Bitte Internetverbindung prüfen."
                    )
                }
            }
        }
    }

    fun selectFaculty(faculty: Faculty?) {
        _uiState.update { current ->
            current.copy(
                selectedFaculty = faculty,
                searchQuery = "",
                filteredCourses = filterCourses(faculty, "")
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredCourses = filterCourses(current.selectedFaculty, query)
            )
        }
    }

    fun completeSetup(course: String) {
        viewModelScope.launch {
            userService.completeSetup(course)
            _uiState.update { current ->
                current.copy(isSetupComplete = true)
            }
        }
    }

    private fun filterCourses(faculty: Faculty?, query: String): List<String> {
        if (faculty == null) return emptyList()

        val facultyPrefix = faculty.prefix.removeSuffix("-")
        return allCoursesCache
            .filter { course -> course.startsWith(facultyPrefix, ignoreCase = true) }
            .filter { course -> course.contains(query, ignoreCase = true) }
            .sorted()
    }

    private fun getCoursesFromLessons(): List<String> {
        return repository.getAllLessons()
            .flatMap { lesson -> lesson.groupsCode }
            .distinct()
    }

    private fun getFacultiesFromCourses(): List<Faculty> {
        return Faculty.entries.filter { faculty ->
            val facultyPrefix = faculty.prefix.removeSuffix("-")
            allCoursesCache.any { course -> course.startsWith(facultyPrefix, ignoreCase = true) }
        }
    }
}
