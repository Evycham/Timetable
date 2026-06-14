package com.example.timetable.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.timetable.data.local.db.dao.EventDao
import com.example.timetable.data.local.db.entities.EventEntity
import com.example.timetable.data.local.db.entities.ExtraLessonEntity
import com.example.timetable.data.local.db.entities.HiddenLessonEntity
import com.example.timetable.data.local.db.dao.LessonDao
import com.example.timetable.data.local.db.entities.LessonEntity
import com.example.timetable.data.local.db.entities.LessonGroupEntity
import com.example.timetable.data.local.db.entities.LessonRoomEntity
import com.example.timetable.data.local.db.entities.LessonTeacherEntity
import com.example.timetable.data.local.db.dao.SyncMetadataDao
import com.example.timetable.data.local.db.entities.SyncMetadataEntity
import com.example.timetable.data.local.db.dao.UserRulesDao

/**
 * Die zentrale Room-Datenbankinstanz der Anwendung.
 *
 * Verwaltet das gesamte lokale Datenbankschema der App, einschließlich:
 * - Dem globalen Vorlesungsplan ([LessonEntity]) und dessen relationalen Zuordnungen
 *   für Räume, Dozenten und Studiengänge.
 * - Lokalen Feiertagen und Sync-Metadaten.
 * - Den benutzerdefinierten Filterregeln ([ExtraLessonEntity], [HiddenLessonEntity]).
 *
 * **Schema-Version 3:**
 * - Version 1: Ursprünglicher Entwurf des Kollegen.
 * - Version 2: Einführung von Sync-Metadaten.
 * - Version 3 (Aktuell): Volle Normalisierung. Auslagerung von Collections in N:M-Zwischentabellen
 *   und Migration der Custom-Regeln (Fremdmodule/Ausblendungen) aus dem Preferences DataStore
 *   in SQLite-Tabellen mit Fremdschlüsseln.
 *
 * Da sich die App noch in der Entwicklungsphase befindet, wird bei Schemaänderungen
 * [fallbackToDestructiveMigration] genutzt. Dadurch wird die DB bei Versions-Updates
 * gelöscht und neu aufgebaut (keine aufwendigen Migrationsskripte notwendig).
 */
@Database(
    entities = [
        LessonEntity::class,
        EventEntity::class,
        SyncMetadataEntity::class,
        LessonRoomEntity::class,
        LessonTeacherEntity::class,
        LessonGroupEntity::class,
        ExtraLessonEntity::class,
        HiddenLessonEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TimetableDatabase : RoomDatabase() {

    /**
     * Zugriff aus DAO-Operationen für Vorlesungen.
     */
    abstract fun lessonDao(): LessonDao

    /**
     * Zugriff auf DAO-Operationen für Feiertage und Events.
     */
    abstract fun eventDao(): EventDao

    /**
     * Zugriff auf DAO-Operationen für Synchronisations-Metadaten.
     */
    abstract fun syncMetadataDao(): SyncMetadataDao

    /**
     * Zugriff auf DAO-Operationen für benutzerspezifische Filterregeln (Fremdmodule/Ausblendungen).
     */
    abstract fun userRulesDao(): UserRulesDao

    companion object {
        private const val DATABASE_NAME = "timetable.db"

        // volatile garantiert sichtbarkeit von änderungen über thread-grenzen hinweg
        @Volatile
        private var instance: TimetableDatabase? = null

        /**
         * Gibt die Singleton-Instanz der Datenbank zurükc.
         *
         * Implementiert das Thread-sichere Singleton-Pattern (Double-Checked Locking), um sicherzustellen, dass nur eine einzige Verbindung zur DB über den gesamten Lebenszyklus der App geöffnet wird.
         *
         * @param context Der Anwendungs-Kontext zur Initialisierung der DB.
         * @return Die Singleton-Instanz von [TimetableDatabase].
         */
        fun getInstance(context: Context): TimetableDatabase =
            // erster check ohne lock
            instance ?: synchronized(this) {
                // zweiter check unter lock
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TimetableDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { database ->
                        instance = database
                    }
            }
    }
}