package com.example.timetable.utils.data.datenmodell

data class LessonSelection(
    val lessonId: String? = null,
    val groupsCode: String? = null,
    val title: String? = null
)

data class HiddenLessonRule(
    val lessonId: String? = null,
    val title: String? = null,
    val groupsCode: String? = null
)

data class UserSchedulePreferences(
    val isSetupComplete: Boolean = false,
    val groupsCode: String? = null,
    val extraLessons: Set<LessonSelection> = emptySet(),
    val hiddenLessons: Set<HiddenLessonRule> = emptySet()
)
