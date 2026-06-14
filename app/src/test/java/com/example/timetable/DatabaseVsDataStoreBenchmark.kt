package com.example.timetable

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.timetable.data.services.LessonParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import kotlin.system.measureTimeMillis

// --- SELF-CONTAINED BENCHMARK MODELS TO RUN ON ANY BRANCH ---
@Entity(tableName = "lessons")
data class BenchmarkLessonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val rooms: String?,
    val building: String?,
    val teacher: String?,
    val groupsCode: String // Serialized JSON array e.g. '["groupsCode-25"]'
)

@Dao
interface BenchmarkLessonDao {
    @Query("SELECT * FROM lessons")
    suspend fun getAll(): List<BenchmarkLessonEntity>

    @Insert
    suspend fun insertAll(lessons: List<BenchmarkLessonEntity>)

    @Query("DELETE FROM lessons")
    suspend fun clear()
}

@Database(entities = [BenchmarkLessonEntity::class], version = 1, exportSchema = false)
abstract class BenchmarkDatabase : RoomDatabase() {
    abstract fun lessonDao(): BenchmarkLessonDao
}

@org.junit.Ignore("Nur als Benchmark gedacht")
@RunWith(RobolectricTestRunner::class)
class DatabaseVsDataStoreBenchmark {

    private val parser = LessonParser()
    private val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    @Test
    fun runBenchmark() = runBlocking {
        println("\n=== BENCHMARK: ROOM DATABASE VS PREFERENCES DATASTORE (1.3 MB DATASET) ===")

        val targetGroup = "groupsCode-25" // Query group (1 of 50 groups)

        // 1. Generate 1.3 MB JSON data distributed across 50 different groups
        val (jsonString, lessonCount) = generateLargeJsonPayload(1.3 * 1024 * 1024, groupCount = 50)
        println("Generated JSON payload:")
        println("- Size: ${"%.2f".format(jsonString.toByteArray(Charsets.UTF_8).size / (1024.0 * 1024.0))} MB")
        println("- Total Lessons: $lessonCount")
        println("- Target Group: $targetGroup")

        val rootJson = JSONObject(jsonString)
        val lessonTimes = rootJson.getJSONObject("result").getJSONObject("displaySchedule")
            .getJSONArray("lessonTimes")

        // 2. Setup Data Sources
        val database = Room.inMemoryDatabaseBuilder(context, BenchmarkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val lessonDao = database.lessonDao()

        val tempDir = Files.createTempDirectory("benchmark-ds").toFile()
        val dataStoreFile = tempDir.resolve("benchmark.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
        val datastoreKey = stringPreferencesKey("timetable_raw_json")

        // Populate database
        val parsedLessons = parser.parseLessons(lessonTimes)
        val entities = parsedLessons.map { lesson ->
            BenchmarkLessonEntity(
                id = lesson.id,
                title = lesson.title,
                date = lesson.date,
                startTime = lesson.startTime,
                endTime = lesson.endTime,
                rooms = lesson.rooms?.joinToString(","),
                building = lesson.building,
                teacher = lesson.teacher?.joinToString(","),
                groupsCode = JSONArray(lesson.groupsCode.toList()).toString()
            )
        }

        database.withTransaction {
            lessonDao.clear()
            if (entities.isNotEmpty()) {
                lessonDao.insertAll(entities)
            }
        }

        // Populate DataStore
        dataStore.edit { preferences ->
            preferences[datastoreKey] = jsonString
        }

        // --- REALISTIC READ BENCHMARK (Querying a Single Course/Group) ---
        println("\n--- REALISTIC READ / QUERY BENCHMARK ---")
        println("(Scenario: User opens their own schedule and displays lessons for group '$targetGroup')")

        // 1. Room DB Read - Selective SQL Query
        // Since we are simulating an optimized database query, we use SQL-like pattern matching.
        // Because groupsCode is stored as a serialized JSON array like '["groupsCode-25"]',
        // we can query it using Room's native SQL with a LIKE query.
        var readLessonsFromRoomCount = 0
        val roomReadTime = measureTimeMillis {
            val queryPattern = "%\"$targetGroup\"%"
            val cursor = database.openHelper.readableDatabase.query(
                "SELECT * FROM lessons WHERE groupsCode LIKE ?",
                arrayOf(queryPattern)
            )

            val matchingTitles = mutableListOf<String>()
            if (cursor.moveToFirst()) {
                do {
                    val titleIndex = cursor.getColumnIndex("title")
                    if (titleIndex >= 0) {
                        matchingTitles.add(cursor.getString(titleIndex))
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            readLessonsFromRoomCount = matchingTitles.size
        }
        println("Room DB SQL-Query: $roomReadTime ms (Database-level filtering, loaded $readLessonsFromRoomCount lessons)")

        // 2. DataStore Read - Full JSON Parse + Kotlin Filter
        // Since DataStore only stores the raw JSON string, we MUST load and parse the ENTIRE file
        // and filter in Kotlin memory on every read.
        var readLessonsFromDsCount = 0
        val dsReadTime = measureTimeMillis {
            val rawJson = dataStore.data.first()[datastoreKey].orEmpty()
            val array = JSONObject(rawJson).getJSONObject("result").getJSONObject("displaySchedule")
                .getJSONArray("lessonTimes")
            val allModels = parser.parseLessons(array)
            val filteredModels = allModels.filter { targetGroup in it.groupsCode }
            readLessonsFromDsCount = filteredModels.size
        }
        println("DataStore JSON-Parse & Filter: $dsReadTime ms (Full file read + full JSON parsing + Kotlin filter, loaded $readLessonsFromDsCount lessons)")

        // Cleanup
        database.close()
        tempDir.deleteRecursively()
        println("\n====================== BENCHMARK END ======================\n")
    }

    private fun generateLargeJsonPayload(
        targetSizeBytes: Double,
        groupCount: Int
    ): Pair<String, Int> {
        val lessonList = JSONArray()
        var estimatedSize = 0
        var count = 0

        // Build single items until size is reached, distributing them among groupsCode-0 to groupsCode-49
        while (estimatedSize < targetSizeBytes) {
            val assignedGroup = "groupsCode-${count % groupCount}"
            val item = JSONObject().apply {
                put("courseTitle", "eti-Autonome Mobile Systeme Vorl. $count")
                put("dates", JSONArray().apply {
                    put("20260615")
                    put("20260622")
                    put("20260629")
                })
                put("startTime", "0945")
                put("endTime", "1115")
                put("classCodes", JSONArray().apply {
                    put(assignedGroup)
                })
                put("roomCodes", JSONArray().apply {
                    put("5/HS2")
                })
                put("teacherCodes", JSONArray().apply {
                    put("eti-Friedenberg")
                })
                put("buildingCodes", JSONArray().apply {
                    put("H5")
                })
            }
            lessonList.put(item)
            estimatedSize += item.toString().length + 1
            count++
        }

        val fullPayload = """
            {
              "result": {
                "displaySchedule": {
                  "lessonTimes": $lessonList,
                  "eventTimes": []
                }
              }
            }
        """.trimIndent()

        return Pair(fullPayload, count * 3) // 3 dates per item
    }
}
