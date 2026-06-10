package com.example.timetable.data.services

import com.example.timetable.data.datenmodell.CalenderDay
import com.example.timetable.data.datenmodell.HiddenLessonRule
import com.example.timetable.data.datenmodell.Lesson
import com.example.timetable.data.datenmodell.LessonSelection
import com.example.timetable.data.datenmodell.UserSchedulePreferences

class UserTimetableService(
    private val repository: TimetableRepository,
    private val preferencesStore: UserSchedulePreferencesStore
) {

    fun getPreferences(): UserSchedulePreferences = preferencesStore.load()

    fun setGroupsCode(groupsCode: String) {
        val current = preferencesStore.load()
        preferencesStore.save(current.copy(groupsCode = groupsCode))
    }

    fun addExtraLesson(groupsCode: String, title: String) {
        val current = preferencesStore.load()
        val updated = current.extraLessons + LessonSelection(groupsCode = groupsCode, title = title)
        preferencesStore.save(current.copy(extraLessons = updated))
    }

    fun addExtraLessonById(lessonId: String) {
        val current = preferencesStore.load()
        val updated = current.extraLessons + LessonSelection(lessonId = lessonId)
        preferencesStore.save(current.copy(extraLessons = updated))
    }

    fun removeExtraLesson(groupsCode: String, title: String) {
        val current = preferencesStore.load()
        val updated = current.extraLessons - LessonSelection(groupsCode = groupsCode, title = title)
        preferencesStore.save(current.copy(extraLessons = updated))
    }

    fun removeExtraLessonById(lessonId: String) {
        val current = preferencesStore.load()
        val updated = current.extraLessons - LessonSelection(lessonId = lessonId)
        preferencesStore.save(current.copy(extraLessons = updated))
    }

    fun hideLesson(groupsCode: String, title: String) {
        val current = preferencesStore.load()
        val updated = current.hiddenLessons + HiddenLessonRule(title = title, groupsCode = groupsCode)
        preferencesStore.save(current.copy(hiddenLessons = updated))
    }

    fun hideLessonById(lessonId: String) {
        val current = preferencesStore.load()
        val updated = current.hiddenLessons + HiddenLessonRule(lessonId = lessonId)
        preferencesStore.save(current.copy(hiddenLessons = updated))
    }

    fun hideAllLessonsByTitle(title: String) {
        val current = preferencesStore.load()
        val updated = current.hiddenLessons + HiddenLessonRule(title = title)
        preferencesStore.save(current.copy(hiddenLessons = updated))
    }

    fun showLesson(groupsCode: String, title: String) {
        val current = preferencesStore.load()
        val updated = current.hiddenLessons - HiddenLessonRule(title = title, groupsCode = groupsCode)
        preferencesStore.save(current.copy(hiddenLessons = updated))
    }

    fun showLessonById(lessonId: String) {
        val current = preferencesStore.load()
        val updated = current.hiddenLessons - HiddenLessonRule(lessonId = lessonId)
        preferencesStore.save(current.copy(hiddenLessons = updated))
    }

    fun showAllLessonsByTitle(title: String) {
        val current = preferencesStore.load()
        val updated = current.hiddenLessons - HiddenLessonRule(title = title)
        preferencesStore.save(current.copy(hiddenLessons = updated))
    }

    fun buildUserLessons(): List<Lesson> {
        val preferences = preferencesStore.load()
        val baseLessons = preferences.groupsCode
            ?.let(repository::getLessonsByGroupsCode)
            .orEmpty()
        val extraLessons = preferences.extraLessons.flatMap { selection ->
            resolveLessonSelection(selection)
        }

        return (baseLessons + extraLessons)
            .distinctBy(Lesson::id)
            .filterNot { lesson -> isHidden(lesson, preferences.hiddenLessons) }
            .sortedWith(compareBy<Lesson> { it.date }.thenBy { it.startTime })
    }

    fun buildUserCalenderDays(): List<CalenderDay> =
        CalenderDayMapper.build(buildUserLessons(), repository.getAllEvents())

    fun clearPreferences() {
        preferencesStore.clear()
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
