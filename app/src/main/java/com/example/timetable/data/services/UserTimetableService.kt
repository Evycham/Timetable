package com.example.timetable.data.services

import com.example.timetable.data.TimetableRepository
import com.example.timetable.data.datenmodell.CalenderDay
import com.example.timetable.data.datenmodell.HiddenLessonRule
import com.example.timetable.data.datenmodell.Lesson
import com.example.timetable.data.datenmodell.LessonSelection
import com.example.timetable.data.datenmodell.UserSchedulePreferences
import com.example.timetable.data.UserSchedulePreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserTimetableService(
    private val repository: TimetableRepository,
    private val preferencesStore: UserSchedulePreferencesStore
) {

    val preferencesFlow: Flow<UserSchedulePreferences> = preferencesStore.preferencesFlow

    fun userLessonsFlow(): Flow<List<Lesson>> =
        preferencesFlow.map(::buildUserLessonsForPreferences)

    fun userCalenderDaysFlow(): Flow<List<CalenderDay>> =
        userLessonsFlow().map { lessons ->
            CalenderDayMapper.build(lessons, repository.getAllEvents())
        }

    suspend fun getPreferences(): UserSchedulePreferences = preferencesStore.load()

    suspend fun setSetupComplete(isSetupComplete: Boolean) {
        preferencesStore.update { current ->
            current.copy(isSetupComplete = isSetupComplete)
        }
    }

    suspend fun setGroupsCode(groupsCode: String) {
        preferencesStore.update { current ->
            current.copy(groupsCode = groupsCode)
        }
    }

    suspend fun completeSetup(groupsCode: String) {
        preferencesStore.update { current ->
            current.copy(
                isSetupComplete = true,
                groupsCode = groupsCode
            )
        }
    }

    suspend fun addExtraLesson(groupsCode: String, title: String) {
        preferencesStore.update { current ->
            current.copy(
                extraLessons = current.extraLessons + LessonSelection(
                    groupsCode = groupsCode,
                    title = title
                )
            )
        }
    }

    suspend fun addExtraLessonById(lessonId: String) {
        preferencesStore.update { current ->
            current.copy(
                extraLessons = current.extraLessons + LessonSelection(lessonId = lessonId)
            )
        }
    }

    suspend fun removeExtraLesson(groupsCode: String, title: String) {
        preferencesStore.update { current ->
            current.copy(
                extraLessons = current.extraLessons - LessonSelection(
                    groupsCode = groupsCode,
                    title = title
                )
            )
        }
    }

    suspend fun removeExtraLessonById(lessonId: String) {
        preferencesStore.update { current ->
            current.copy(
                extraLessons = current.extraLessons - LessonSelection(lessonId = lessonId)
            )
        }
    }

    suspend fun hideLesson(groupsCode: String, title: String) {
        preferencesStore.update { current ->
            current.copy(
                hiddenLessons = current.hiddenLessons + HiddenLessonRule(
                    title = title,
                    groupsCode = groupsCode
                )
            )
        }
    }

    suspend fun hideLessonById(lessonId: String) {
        preferencesStore.update { current ->
            current.copy(
                hiddenLessons = current.hiddenLessons + HiddenLessonRule(lessonId = lessonId)
            )
        }
    }

    suspend fun showLesson(groupsCode: String, title: String) {
        preferencesStore.update { current ->
            current.copy(
                hiddenLessons = current.hiddenLessons - HiddenLessonRule(
                    title = title,
                    groupsCode = groupsCode
                )
            )
        }
    }

    suspend fun showLessonById(lessonId: String) {
        preferencesStore.update { current ->
            current.copy(
                hiddenLessons = current.hiddenLessons - HiddenLessonRule(lessonId = lessonId)
            )
        }
    }

    suspend fun showAllLessonsByTitle(title: String) {
        preferencesStore.update { current ->
            current.copy(
                hiddenLessons = current.hiddenLessons - HiddenLessonRule(title = title)
            )
        }
    }

    suspend fun buildUserLessons(): List<Lesson> =
        buildUserLessonsForPreferences(preferencesStore.load())

    suspend fun buildUserCalenderDays(): List<CalenderDay> =
        CalenderDayMapper.build(buildUserLessons(), repository.getAllEvents())

    suspend fun clearPreferences() {
        preferencesStore.clear()
    }

    private fun buildUserLessonsForPreferences(preferences: UserSchedulePreferences): List<Lesson> {
        val baseLessons = preferences.groupsCode
            ?.let(repository::getLessonsByGroupsCode)
            .orEmpty()
        val extraLessons = preferences.extraLessons.flatMap(::resolveLessonSelection)

        return (baseLessons + extraLessons)
            .distinctBy(Lesson::id)
            .filterNot { lesson -> isHidden(lesson, preferences.hiddenLessons) }
            .sortedWith(compareBy<Lesson> { it.date }.thenBy { it.startTime })
    }

    private fun resolveLessonSelection(selection: LessonSelection): List<Lesson> {
        val lessonId = selection.lessonId
        if (lessonId != null) {
            return repository.getLessonById(lessonId)?.let(::listOf).orEmpty()
        }

        val title = selection.title
        val groupsCode = selection.groupsCode
        if (title != null && groupsCode != null) {
            return repository.getLessonsByTitleAndGroupsCode(
                title = title,
                groupsCode = groupsCode
            )
        }

        return emptyList()
    }

    private fun isHidden(lesson: Lesson, hiddenRules: Set<HiddenLessonRule>): Boolean =
        hiddenRules.any { rule ->
            when {
                rule.lessonId != null -> rule.lessonId == lesson.id
                rule.title != null -> {
                    rule.title == lesson.title &&
                        (rule.groupsCode == null || rule.groupsCode in lesson.groupsCode)
                }

                else -> false
            }
        }
}
