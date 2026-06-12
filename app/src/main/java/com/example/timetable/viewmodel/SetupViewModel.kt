package com.example.timetable.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timetable.utils.enums.Faculty
import com.example.timetable.view.json.JsonLessonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SetupUiState(
    val faculties: List<Faculty> = emptyList(),
    val selectedFaculty: Faculty? = null,
    val searchQuery: String = "",
    val filteredCourses: List<String> = emptyList(),
    val isSetupComplete: Boolean = false
)

private val Context.setupDataStore by preferencesDataStore(name = "setup_preferences")

class SetupViewModel(
    private val repository: JsonLessonRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SetupUiState(faculties = loadFaculties())
    )
    val uiState: StateFlow<SetupUiState> = _uiState

    fun selectFaculty(faculty: Faculty) {
        _uiState.value = _uiState.value.copy(
            selectedFaculty = faculty,
            searchQuery = "",
            filteredCourses = filterCourses(faculty, "")
        )
    }

    fun clearFacultySelection() {
        _uiState.value = _uiState.value.copy(
            selectedFaculty = null,
            searchQuery = "",
            filteredCourses = emptyList()
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredCourses = filterCourses(_uiState.value.selectedFaculty, query)
        )
    }

    fun completeSetup(course: String, onSetupComplete: () -> Unit = {}) {
        viewModelScope.launch {
            context.setupDataStore.edit { preferences ->
                preferences[IS_SETUP_COMPLETE] = true
                preferences[SELECTED_COURSE] = course
            }
            _uiState.value = _uiState.value.copy(isSetupComplete = true)
            onSetupComplete()
        }
    }

    fun getLessonsForCourse(course: String) = repository.getLessonsByCourse(course)

    private fun filterCourses(faculty: Faculty?, query: String): List<String> {
        if (faculty == null) return emptyList()

        return repository.getCoursesByFaculty(faculty.prefix)
            .filter { it.contains(query, ignoreCase = true) }
    }

    private fun loadFaculties(): List<Faculty> {
        val facultyCodes = repository.getFaculties()
            .map { it.code.lowercase() }
            .toSet()

        if (facultyCodes.isEmpty()) return Faculty.entries.toList()

        return Faculty.entries.filter { faculty ->
            faculty.prefix.removeSuffix("-").lowercase() in facultyCodes
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext

            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SetupViewModel(
                        repository = JsonLessonRepository(appContext),
                        context = appContext
                    ) as T
                }
            }
        }

        private val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        private val SELECTED_COURSE = stringPreferencesKey("selected_course")
    }
}
