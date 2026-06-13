package com.example.timetable.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timetable.data.local.db.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY startDate, endDate, title")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startDate, endDate, title")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clear()
}