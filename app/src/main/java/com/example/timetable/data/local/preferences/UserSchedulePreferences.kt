package com.example.timetable.data.local.preferences

/**
 * Speichert einfache Benutzereinstellungen im Preferences DataStore.
 *
 * Hinweis für die Architektur:
 * Komplexe Filterregeln habe ich aus dieser Klasse entfernt und in Room verschoben. Das verhindert das O(N*M) Problem und garantiert Datenkonsistenz.
 *
 * @property isSetupComplete Zeigt an, ob der Nutzer den Setup-Screen durchlaufen hat.
 * @property groupsCode Der primär gewählte Studiengang des Nutzers
 * @property isDynamicColorEnabled Aktiviert das dynamische Farbschema der App (über Material You)
 * @property isCancellationAlertEnabled Steuert, ob Benachrichtigungen bei Vorlesungsausfällen aktiv sind
 * @property isRoomChangeAlertEnabled Steuert, ob Benachrichtigungen bei Raumänderungen aktiv sind
 * @property moduleEmojis Speichert die für Veranstaltungen gewählten Emojis
 */
data class UserSchedulePreferences(
    val isSetupComplete: Boolean = false,
    val groupsCode: String? = null,
    val isDynamicColorEnabled: Boolean = false,
    val isCancellationAlertEnabled: Boolean = true,
    val isRoomChangeAlertEnabled: Boolean = true,
    val moduleEmojis: Map<String, String> = emptyMap()
)