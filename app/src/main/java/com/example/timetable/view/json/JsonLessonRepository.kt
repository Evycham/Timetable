package com.example.timetable.view.json

import android.content.Context
import org.json.JSONObject

class JsonLessonRepository(private val context: Context? = null) {

    private var timetableData: TimetableData? = null

    init {
        context?.let { loadData(it) }
    }

    private fun loadData(context: Context) {
        try {
            val jsonString = context.assets.open("test-short.json").bufferedReader().use { it.readText() }
            timetableData = parseJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Parsed den JSON-String in die internen Modelle.
     * Öffentlich für Unit-Tests.
     */
    fun parseJson(jsonString: String): TimetableData {
        val root = JSONObject(jsonString)
        
        val facultiesJson = root.getJSONArray("faculties")
        val facultiesList = mutableListOf<JsonFaculty>()
        for (i in 0 until facultiesJson.length()) {
            val f = facultiesJson.getJSONObject(i)
            facultiesList.add(JsonFaculty(
                code = f.getString("code"),
                name = f.getString("name"),
                color = f.getString("color")
            ))
        }

        val lessonsJson = root.getJSONArray("lessons")
        val lessonsList = mutableListOf<JsonLesson>()
        for (i in 0 until lessonsJson.length()) {
            val l = lessonsJson.getJSONObject(i)
            val changeJson = l.optJSONObject("change")
            val change = if (changeJson != null) {
                JsonLessonChange(
                    caption = changeJson.getString("caption"),
                    message = changeJson.getString("message")
                )
            } else null

            lessonsList.add(JsonLesson(
                id = l.getString("id"),
                title = l.getString("title"),
                date = l.getString("date"),
                startTime = l.getString("startTime"),
                endTime = l.getString("endTime"),
                room = l.getString("room"),
                lecturer = l.getString("lecturer"),
                course = l.getString("course"),
                change = change
            ))
        }

        val data = TimetableData(facultiesList, lessonsList)
        this.timetableData = data
        return data
    }

    fun getFaculties(): List<JsonFaculty> = timetableData?.faculties ?: emptyList()

    /**
     * Extrahiert alle eindeutigen Kurse aus allen Lessons.
     */
    fun getAllCourses(): List<String> {
        return timetableData?.lessons?.flatMap { it.course.split(",").map { c -> c.trim() } }?.distinct() ?: emptyList()
    }

    /**
     * Gibt Kurse zurück, die mit einem bestimmten Präfix beginnen.
     */
    fun getCoursesByFaculty(prefix: String): List<String> {
        return getAllCourses().filter { it.startsWith(prefix, ignoreCase = true) }
    }

    /**
     * Gibt alle Lessons zurück, die zu einem Kurs-String passen.
     */
    fun getLessonsByCourse(courseName: String): List<JsonLesson> {
        return timetableData?.lessons?.filter { lesson ->
            lesson.course.split(",").map { it.trim() }.contains(courseName)
        } ?: emptyList()
    }

    /**
     * Gibt alle eindeutigen Module (Titel) zurück.
     */
    fun getAllModuleTitles(): List<String> {
        return timetableData?.lessons?.map { it.title }?.distinct() ?: emptyList()
    }

    /**
     * Sucht nach Modulen basierend auf verschiedenen Parametern.
     */
    fun searchModules(query: String): List<JsonLesson> {
        if (query.isBlank()) return emptyList()
        val allLessons = timetableData?.lessons ?: return emptyList()
        
        return allLessons.filter { lesson ->
            lesson.title.contains(query, ignoreCase = true) ||
            lesson.lecturer.contains(query, ignoreCase = true) ||
            lesson.room.contains(query, ignoreCase = true) ||
            lesson.course.contains(query, ignoreCase = true)
        }.distinctBy { it.title }
    }

    /**
     * Gibt alle Instanzen (Lessons) eines Modul-Titels zurück.
     */
    fun getLessonsByModuleTitle(title: String): List<JsonLesson> {
        return timetableData?.lessons?.filter { it.title == title } ?: emptyList()
    }
}
