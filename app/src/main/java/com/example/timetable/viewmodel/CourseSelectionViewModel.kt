package com.example.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.model.Lesson
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseSelectionViewModel(
    repository: TimetableRepository,
    private val userService: UserTimetableService
) : ViewModel() {

    private val allLessons: StateFlow<List<Lesson>> =
        repository.lessonsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<Lesson>> =
        combine(_searchQuery, allLessons) { query, lessons ->
            val trimmed = query.trim()
            if (trimmed.isBlank()) {
                emptyList()
            } else {
                lessons
                    .filter { lesson -> lesson.title.contains(trimmed, ignoreCase = true) }
                    .distinctBy { lesson -> lesson.title to lesson.groupsCode }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val extraLessons: StateFlow<List<Lesson>> =
        combine(
            userService.userLessonsFlow(),
            userService.preferencesFlow
        ) { lessons, preferences ->
            val groupsCode = preferences.groupsCode
            if (groupsCode == null) {
                emptyList()
            } else {
                lessons
                    .filter { lesson -> !lesson.groupsCode.contains(groupsCode) }
                    .distinctBy { lesson -> lesson.id }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenLessons: StateFlow<List<Lesson>> =
        combine(
            allLessons,
            userService.userLessonsFlow(),
            userService.preferencesFlow
        ) { lessons, userLessons, preferences ->
            val groupsCode = preferences.groupsCode
            if (groupsCode == null) {
                emptyList()
            } else {
                val visibleLessonIds = userLessons.map { lesson -> lesson.id }.toSet()
                lessons
                    .filter { lesson -> lesson.groupsCode.contains(groupsCode) }
                    .filter { lesson -> !visibleLessonIds.contains(lesson.id) }
                    .distinctBy { lesson -> lesson.id }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchModules(query: String) {
        _searchQuery.value = query
    }

    fun addExtraModule(groupsCode: String, title: String) {
        viewModelScope.launch {
            userService.addExtraLesson(groupsCode, title)
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

    fun showModule(groupsCode: String, title: String) {
        viewModelScope.launch {
            userService.showLesson(groupsCode, title)
        }
    }

    fun hideSingleLesson(lessonId: String) {
        viewModelScope.launch {
            userService.hideLessonById(lessonId)
        }
    }

    fun showSingleLesson(lessonId: String) {
        viewModelScope.launch {
            userService.showLessonById(lessonId)
        }
    }
}
