package com.example.timetable.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import org.json.JSONObject

/**
 * Globale Erweiterung zur Bereitstellung der DataStore-Instanz
 * Ermöglicht den Aufruf von `conext.usesSchedulePreferenceDataStore` überall in der App.
 * Nutzt Delegate-Delegation, um sicherzustellen, dass nur der Speicher als Singleton (max 1 Instanz) initialisiert ist.
 */
val Context.userSchedulePreferencesDataStore by preferencesDataStore(
    name = UserSchedulePreferencesStore.DATASTORE_NAME
)

/**
 * Persistenter Speicher für Benutzereinstellungen (nutzt JetPack Datastore)
 *
 * @param dataStore Die zugrunde liegende Datastore-Instanz
 */
class UserSchedulePreferencesStore(
    private val dataStore: DataStore<Preferences>
) {

    @Volatile
    private var _cachedPreferences: UserSchedulePreferences = UserSchedulePreferences()
    val cachedPreferences: UserSchedulePreferences get() = _cachedPreferences

    /**
     * Ein reaktiver Stream der aktuellen Benutzereinstellungen.
     * Fängt IOExceptions ab und emittiert leere Einstellungen als Fallback.
     */
    val preferencesFlow: Flow<UserSchedulePreferences> = dataStore.data
        .catch { exception ->
            // dateizugriffsfehler abfangen
            if (exception is IOException) {
                // leere standardeinstellungen bei lesefehlern
                emit(emptyPreferences())
            } else {
                // laufzeit- / programmierfehler weiterwerfen
                throw exception
            }
        }
        .map(::mapPreferences)

    /**
     * Lädt den aktuellen Stand der Benutzereinstellungen, ohne den UI-Thread zu blockieren.
     *
     * @return Das aktuelle [UserSchedulePreferences] Objekt.
     */
    suspend fun load(): UserSchedulePreferences = preferencesFlow.first()

    /**
     * Überschreibt die aktuellen Benutzereinstellungen im Speicher.
     *
     * @param preferences Das neue Einstellungs-Objekt, das gespeichert werden soll.
     */
    suspend fun save(preferences: UserSchedulePreferences) {
        // edit blockiert den schreibzugriff, um transaktionalität zu sichern
        dataStore.edit { store ->
            writePreferences(store, preferences)
        }
    }

    /**
     * Aktualisiert die aktuellen Benutzereinstellungen transaktional über eine Lambda-Funktion.
     *
     * @param transform Lambda-Funktion, die die aktuellen Einstellungen erhält und die aktualisierte zurückgibt.
     */
    suspend fun update(transform: (UserSchedulePreferences) -> UserSchedulePreferences) {
        // edit für Transaktionalität
        dataStore.edit { store ->
            // 1. aktuellen stand von disk lesen
            val current = mapPreferences(store)
            // 2. übergebene transformationen auf den alten zustand anwenden
            val updated = transform(current)
            // 3. modifizierten state zurück in die preferences schreiben
            writePreferences(store, updated)
        }
    }

    /**
     * Löscht alle gespeicherten Key-Value-Einstellungen.
     */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /**
     * Mappt das DataStore-[Preferences]-Objekt in unsere Domänenklasse [UserSchedulePreferences].
     */
    private fun mapPreferences(preferences: Preferences): UserSchedulePreferences {
        return UserSchedulePreferences(
            // elvis operator (?: ) definiert fallback
            preferences[IS_SETUP_COMPLETE_KEY] ?: false,
            preferences[GROUPS_CODE_KEY],
            preferences[IS_DYNAMIC_COLOR_KEY] ?: false,
            preferences[IS_CANCELLATION_ALERT_KEY] ?: true,
            preferences[IS_ROOM_CHANGE_ALERT_KEY] ?: true,
            parseEmojis(preferences[MODULE_EMOJIS_KEY]),
            preferences[APP_FONT_SIZE_KEY] ?: "Mittel"
        ).also {
            _cachedPreferences = it
        }
    }

    /**
     * Schreibt die Domänenklasse [UserSchedulePreferences] zurück in das DataStore-[MutablePreferences]-Objekt.
     * Wenn ein String null oder leer ist, löschen wir den Key komplett, um Speicherplatz zu sparen.
     */
    private fun writePreferences(
        store: MutablePreferences,
        preferences: UserSchedulePreferences
    ) {
        _cachedPreferences = preferences
        store[IS_SETUP_COMPLETE_KEY] = preferences.isSetupComplete
        if (preferences.groupsCode.isNullOrBlank()) {
            store.remove(GROUPS_CODE_KEY)
        } else {
            store[GROUPS_CODE_KEY] = preferences.groupsCode
        }

        // die restlichen attribute in den store schreiben
        store[IS_DYNAMIC_COLOR_KEY] = preferences.isDynamicColorEnabled
        store[IS_CANCELLATION_ALERT_KEY] = preferences.isCancellationAlertEnabled
        store[IS_ROOM_CHANGE_ALERT_KEY] = preferences.isRoomChangeAlertEnabled
        if (preferences.moduleEmojis.isEmpty()) {
            store.remove(MODULE_EMOJIS_KEY)
        } else {
            store[MODULE_EMOJIS_KEY] = serializeEmojis(preferences.moduleEmojis)
        }
        store[APP_FONT_SIZE_KEY] = preferences.appFontSize
    }

    /**
     * Parst die JSON-Zeichenkette der Emojis in eine Map (Modulname -> Emoji).
     *
     * @param jsonStr Der serialisierte JSON-String aus dem DataStore.
     * @return Eine Map mit den Emojis oder eine leere Map bei Fehlern/leeren Daten.
     */
    private fun parseEmojis(jsonStr: String?): Map<String, String> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        return try {
            val jsonObject = JSONObject(jsonStr)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.optString(key) ?: ""
                map[key] = value
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Serialisiert die Emoji-Map in eine JSON-Zeichenkette zur Speicherung im DataStore.
     *
     * @param emojis Die zu serialisierende Map (Modulname -> Emoji).
     * @return Die JSON-Zeichenkette.
     */
    private fun serializeEmojis(emojis: Map<String, String>): String {
        val jsonObject = JSONObject()
        for ((key, value) in emojis) {
            jsonObject.put(key, value)
        }
        return jsonObject.toString()
    }

    companion object {
        const val DATASTORE_NAME = "user_schedule_preferences"

        private val IS_SETUP_COMPLETE_KEY = booleanPreferencesKey("is_setup_complete")
        private val GROUPS_CODE_KEY = stringPreferencesKey("groups_code")
        private val IS_DYNAMIC_COLOR_KEY = booleanPreferencesKey("is_dynamic_color")
        private val IS_CANCELLATION_ALERT_KEY = booleanPreferencesKey("is_cancellation_alert")
        private val IS_ROOM_CHANGE_ALERT_KEY = booleanPreferencesKey("is_room_change_alert")
        private val MODULE_EMOJIS_KEY = stringPreferencesKey("module_emojis")
        private val APP_FONT_SIZE_KEY = stringPreferencesKey("app_font_size")
    }
}
