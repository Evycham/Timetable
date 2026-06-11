package com.example.timetable.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val jsonHash: String,
    val jsonSize: Long,
    val syncedAtMillis: Long
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
