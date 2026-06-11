package com.example.timetable.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [LessonEntity::class, EventEntity::class, SyncMetadataEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class TimetableDatabase : RoomDatabase() {

    abstract fun lessonDao(): LessonDao

    abstract fun eventDao(): EventDao

    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        private const val DATABASE_NAME = "timetable.db"

        @Volatile
        private var instance: TimetableDatabase? = null

        fun getInstance(context: Context): TimetableDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TimetableDatabase::class.java,
                    DATABASE_NAME
                ).build().also { database ->
                    instance = database
                }
            }
    }
}
