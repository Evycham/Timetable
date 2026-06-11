# Timetable Data Guide

## Ziel dieses Dokuments

Dieses Dokument beschreibt die Datenarchitektur der App.
Es ist für Entwickler gedacht, die später UI, ViewModel oder weitere Features mit dem aktuellen Datenmodell verbinden.

Der Fokus liegt auf diesen Fragen:

- Woher kommen die Stundenplandaten?
- Wo werden sie gespeichert?
- Welche Klasse ist für was zuständig?
- Welche Methoden müssen in der UI wirklich benutzt werden?
- Wie baut man daraus den persönlichen Stundenplan eines Users?

## Architektur auf einen Blick

Der aktuelle Datenfluss ist bewusst getrennt:

1. `DaVinciApi`
   Lädt die aktuelle JSON vom Server.

2. `LessonParser`
   Parsed die JSON in App-Modelle wie `Lesson` und `Event`.

3. `TimetableRepository`
   Ist die zentrale Datenquelle für alle globalen Stundenplandaten.
   Diese Klasse lädt aus Room, aktualisiert bei Bedarf aus dem Netz und stellt Listen sowie Flows bereit.

4. `UserSchedulePreferencesStore`
   Speichert die user-spezifischen Einstellungen in DataStore.

5. `UserTimetableService`
   Kombiniert die globalen Repository-Daten mit den User-Preferences und baut daraus den persönlichen Stundenplan.

6. `CalenderDayMapper`
   Baut aus `Lesson` und `Event` eine kalenderfreundliche Liste von `CalenderDay`.

## Wichtige Grundidee

Es gibt zwei Datenebenen:

### 1. Globale Stundenplandaten

Das sind alle `Lesson`- und `Event`-Einträge aus DaVinci.
Diese Daten gelten für alle User und liegen lokal in Room.

### 2. User-spezifische Auswahl

Das sind nur persönliche Einstellungen:

- welcher `groupsCode` gewählt wurde
- welche zusätzlichen Lessons aufgenommen werden
- welche Lessons ausgeblendet werden
- ob das Setup schon abgeschlossen wurde

Diese Daten liegen in DataStore.

Das ist wichtig:

Der persönliche Stundenplan wird nicht als eigene Datenbank gespeichert.
Er wird immer aus globalen Daten plus Preferences berechnet.

## Datenmodelle

### `Lesson`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/Lessons.kt`

Wichtige Felder:

- `id`
- `title`
- `date`
- `startTime`
- `endTime`
- `rooms`
- `building`
- `teacher`
- `groupsCode`
- `change`

Wichtig:

`id` ist stabil und wird im Parser deterministisch gebaut.
Dadurch kann eine konkrete einzelne Lesson über mehrere App-Starts hinweg wiedergefunden werden.
Das ist wichtig für User-Regeln wie:

- nur eine bestimmte Woche ausblenden
- nur genau eine bestimmte Lesson zusätzlich anzeigen

### `Event`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/Events.kt`

Wichtige Felder:

- `id`
- `title`
- `startDate`
- `endDate`
- `category`

Events können mehrere Tage umfassen.
Die Verteilung auf einzelne Kalendertage macht später `CalenderDayMapper`.

### `CalenderDay`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/CalenderDay.kt`

Ein `CalenderDay` ist die UI-freundliche Tagesansicht.

Felder:

- `date`
- `lessons`
- `events`

Die UI sollte später meistens mit `List<CalenderDay>` arbeiten und nicht direkt mit rohen JSON-Daten.

### `UserSchedulePreferences`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/UserSchedulePreferences.kt`

Felder:

- `isSetupComplete`
- `groupsCode`
- `extraLessons`
- `hiddenLessons`

Zusätzliche Modelle:

- `LessonSelection`
- `HiddenLessonRule`

Diese beiden Typen beschreiben keine kompletten Lessons, sondern nur User-Regeln.
Das ist absichtlich so, damit Preferences klein und stabil bleiben.

## Zuständigkeiten der Klassen

## `DaVinciApi`

Datei: `app/src/main/java/com/example/timetable/data/services/DaVinciApi.kt`

Zweck:

- lädt die DaVinci-JSON aus dem Netz
- berechnet Hash und Größe der Antwort
- parsed das grobe Root-JSON in `DaVinciResponse`

Wichtige Methode:

- `downloadSnapshot()`

Diese Methode liefert:

- `rawJson`
- `response`
- `jsonSize`
- `jsonHash`

Wichtig:

`DaVinciApi` speichert nichts lokal.
Lokale Speicherung passiert im `TimetableRepository` über Room.

## `LessonParser`

Datei: `app/src/main/java/com/example/timetable/data/services/LessonParser.kt`

Zweck:

- `lessonTimes` aus der API in `Lesson` umwandeln
- `eventTimes` aus der API in `Event` umwandeln
- Daten und Zeiten formatieren
- stabile Lesson-IDs erzeugen

Wichtige Methoden:

- `parseLessons(...)`
- `parseLesson(...)`
- `parseEvents(...)`
- `parseEvent(...)`
- `parseChange(...)`

Wichtig:

Der Parser gruppiert nicht.
Er erzeugt nur saubere Kotlin-Modelle.
Kalenderlogik gehört nicht in den Parser.

## `CalenderDayMapper`

Datei: `app/src/main/java/com/example/timetable/data/services/CalenderDayMapper.kt`

Zweck:

- Lessons nach Datum gruppieren
- Lessons pro Tag nach Startzeit sortieren
- Events auf alle Tage zwischen `startDate` und `endDate` verteilen
- daraus `List<CalenderDay>` bauen

Wichtige Methode:

- `build(lessons, events)`

Die UI sollte später fast immer das Ergebnis dieser Methode anzeigen.

## `TimetableRepository`

Datei: `app/src/main/java/com/example/timetable/data/TimetableRepository.kt`

Dies ist die wichtigste globale Datenklasse.

Zweck:

- lädt Stundenplandaten aus Room
- lädt neue Daten aus dem Netz
- aktualisiert Room bei Änderungen
- hält die aktuell geladenen Listen zusätzlich im Speicher
- stellt Flows für UI oder Service bereit

### Lokale Speicherung

`TimetableRepository` speichert in Room:

- `LessonEntity`
- `EventEntity`
- `SyncMetadataEntity`

`SyncMetadataEntity` enthält:

- `jsonHash`
- `jsonSize`
- `syncedAtMillis`

Damit kann geprüft werden, ob neue Serverdaten wirklich anders sind.

### Wichtige Methoden

#### `initialize()`

Verwendung:

- App-Start
- erster Einstieg ins Repository

Verhalten:

- wenn Daten in Room vorhanden sind, werden sie aus Room gelesen
- wenn Room leer ist, wird aus dem Netz geladen und anschließend in Room gespeichert

Diese Methode ist die normale Startmethode.

#### `loadFromCache()`

Verwendung:

- wenn man explizit nur den lokalen DB-Stand laden will

Verhalten:

- liest nur aus Room
- macht keinen Netzaufruf
- gibt `null` zurück, wenn die DB leer ist

#### `reloadJson()`

Verwendung:

- manuelles hartes Refresh
- Pull-to-refresh
- Debugzwecke

Verhalten:

- lädt neu aus dem Netz
- parsed neu
- ersetzt den kompletten Room-Inhalt
- aktualisiert den In-Memory-Zustand

#### `updateJsonIfNeeded()`

Verwendung:

- regelmäßige Update-Prüfung
- App-Start nach `initialize()`
- Hintergrundsync

Verhalten:

- lädt neues JSON vom Server
- vergleicht `jsonHash` mit gespeicherten Metadaten
- ersetzt Room nur, wenn sich die Daten wirklich geändert haben
- bei Offline-Fehler bleibt der letzte lokale DB-Stand erhalten

Rückgabe:

- `true` wenn neue Daten übernommen wurden
- `false` wenn nichts geändert wurde oder nur lokaler Fallback verwendet wurde

### Wichtige Getter

- `getAllLessons()`
- `getAllEvents()`
- `getAllCalenderDays()`
- `getLessonsByGroupsCode(groupsCode)`
- `getLessonById(lessonId)`
- `getLessonsByTitleAndGroupsCode(title, groupsCode)`
- `getLessonsByDate(date)`
- `getEventsByDate(date)`
- `getCalenderDay(date)`

Diese Getter arbeiten auf dem aktuell geladenen In-Memory-Zustand.
Darum sollte vor der ersten Benutzung immer `initialize()` aufgerufen worden sein.

### Flows aus dem Repository

- `lessonsFlow`
- `eventsFlow`
- `calenderDaysFlow`

Diese Flows lesen aus Room und sind für reaktive UI-Anbindung gedacht.

## `UserSchedulePreferencesStore`

Datei: `app/src/main/java/com/example/timetable/data/UserSchedulePreferencesStore.kt`

Zweck:

- speichert User-Einstellungen in DataStore
- liefert einen `Flow<UserSchedulePreferences>`

Wichtige API:

- `preferencesFlow`
- `load()`
- `save(...)`
- `update(...)`
- `clear()`

Wichtig:

User-Einstellungen sind nicht dieselben Daten wie der Stundenplan.
Preferences speichern nur Regeln und Auswahl, nicht die kompletten Stundenplandaten.

## `UserTimetableService`

Datei: `app/src/main/java/com/example/timetable/data/services/UserTimetableService.kt`

Dies ist die wichtigste Klasse für den persönlichen Stundenplan.

Zweck:

- liest globale Stundenplandaten aus `TimetableRepository`
- liest User-Einstellungen aus `UserSchedulePreferencesStore`
- baut daraus die user-spezifische Anzeige

### Flows

- `preferencesFlow`
- `userLessonsFlow()`
- `userCalenderDaysFlow()`

Diese Flows sind für die UI-Anbindung besonders wichtig.

### Setup-Funktionen

- `setSetupComplete(isSetupComplete)`
- `setGroupsCode(groupsCode)`
- `completeSetup(groupsCode)`

Typischer Fall:

Nach dem Setup-Screen sollte meistens `completeSetup(groupsCode)` verwendet werden.

### Extra-Lessons verwalten

- `addExtraLesson(groupsCode, title)`
- `addExtraLessonById(lessonId)`
- `removeExtraLesson(groupsCode, title)`
- `removeExtraLessonById(lessonId)`

Bedeutung:

Extra-Lessons sind zusätzliche Einträge, die nicht automatisch über den Haupt-`groupsCode` kommen.

### Lessons ausblenden oder wieder anzeigen

- `hideLesson(groupsCode, title)`
- `hideLessonById(lessonId)`
- `showLesson(groupsCode, title)`
- `showLessonById(lessonId)`
- `showAllLessonsByTitle(title)`

Bedeutung:

Damit kann ein User:

- eine konkrete einzelne Lesson ausblenden
- alle Lessons eines Moduls in einem bestimmten Code ausblenden
- globale Titelregeln wieder entfernen

### User-Stundenplan bauen

- `buildUserLessons()`
- `buildUserCalenderDays()`

Diese Methoden sind gut für direkte `suspend`-Aufrufe.
Für reaktive UI ist aber `userLessonsFlow()` oder `userCalenderDaysFlow()` meist besser.

## Datenfluss im Detail

## Erstes Laden der App

1. UI oder ViewModel erzeugt `TimetableRepository`
2. `initialize()` wird aufgerufen
3. Repository prüft Room
4. wenn Room leer ist, wird aus dem Netz geladen
5. Parser erzeugt `Lesson` und `Event`
6. Repository speichert alles in Room
7. Repository baut `CalenderDay`
8. UI kann Daten anzeigen

## Späterer App-Start ohne Internet

1. Repository wird erzeugt
2. `initialize()` wird aufgerufen
3. Room enthält bereits Daten
4. Daten werden lokal geladen
5. UI funktioniert ohne Netz

## Update-Prüfung

1. `updateJsonIfNeeded()` wird aufgerufen
2. `DaVinciApi` lädt neues JSON
3. Repository vergleicht `jsonHash` mit `SyncMetadataEntity`
4. wenn verschieden:
   neue Daten werden geparst und Room ersetzt
5. wenn gleich:
   bestehende Daten bleiben erhalten

## Persönlicher Stundenplan eines Users

1. User wählt einen `groupsCode`
2. der Code wird in DataStore gespeichert
3. `UserTimetableService` liest globale Lessons aus dem Repository
4. Basis-Lessons werden nach `groupsCode` gefiltert
5. `extraLessons` werden zusätzlich aufgelöst
6. `hiddenLessons` werden herausgefiltert
7. daraus wird `List<CalenderDay>` gebaut

## Was die UI später wirklich benutzen sollte

Die UI sollte nicht direkt mit Parser oder DAOs arbeiten.
Die UI sollte je nach Fall nur mit diesen Klassen arbeiten:

- `TimetableRepository`
- `UserTimetableService`
- `UserSchedulePreferencesStore` nur indirekt über den Service oder Setup-Logik

## Empfohlener UI-Startablauf

### Für globale Daten

Im `ViewModel` oder App-Start:

```kotlin
class TimetableViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = TimetableRepository(
        context = application.applicationContext
    )

    val calenderDays = repository.calenderDaysFlow

    init {
        viewModelScope.launch {
            repository.initialize()
            repository.updateJsonIfNeeded()
        }
    }
}
```

### Für den persönlichen Stundenplan

```kotlin
class UserTimetableViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = TimetableRepository(
        context = application.applicationContext
    )

    private val preferencesStore = UserSchedulePreferencesStore(
        application.applicationContext.userSchedulePreferencesDataStore
    )

    private val userService = UserTimetableService(
        repository = repository,
        preferencesStore = preferencesStore
    )

    val userCalenderDays = userService.userCalenderDaysFlow()
    val preferences = userService.preferencesFlow

    init {
        viewModelScope.launch {
            repository.initialize()
            repository.updateJsonIfNeeded()
        }
    }
}
```

### In Compose

```kotlin
@Composable
fun TimetableScreen(viewModel: UserTimetableViewModel) {
    val days by viewModel.userCalenderDays.collectAsState(initial = emptyList())

    // days rendern
}
```

## Welche Methode wann benutzt werden sollte

### Bei App-Start

Nutzen:

- `repository.initialize()`

Optional direkt danach:

- `repository.updateJsonIfNeeded()`

### Für Setup-Screen

Nutzen:

- `userService.completeSetup(groupsCode)`

### Für normales Anzeigen des persönlichen Stundenplans

Nutzen:

- `userService.userCalenderDaysFlow()`

### Für einmalige direkte Abfragen

Nutzen:

- `repository.getLessonsByGroupsCode(...)`
- `repository.getLessonById(...)`
- `repository.getLessonsByTitleAndGroupsCode(...)`
- `userService.buildUserLessons()`
- `userService.buildUserCalenderDays()`

### Für manuelles Refresh

Nutzen:

- `repository.reloadJson()`

### Für stilles Hintergrund-Update

Nutzen:

- `repository.updateJsonIfNeeded()`

## Typische Anwendungsfälle

## 1. User wählt seinen Studiengang zum ersten Mal

```kotlin
viewModelScope.launch {
    userService.completeSetup("mb-MBB_4")
}
```

## 2. User fügt ein Fremdmodul hinzu

Wenn die Auswahl über Titel und Code läuft:

```kotlin
viewModelScope.launch {
    userService.addExtraLesson(
        groupsCode = "eti-SKIB_4",
        title = "Informatik"
    )
}
```

Wenn die Auswahl über eine konkrete Lesson laufen soll:

```kotlin
viewModelScope.launch {
    userService.addExtraLessonById(lessonId)
}
```

## 3. User blendet nur eine bestimmte Woche aus

```kotlin
viewModelScope.launch {
    userService.hideLessonById(lessonId)
}
```

## 4. User blendet ein Modul eines bestimmten Codes aus

```kotlin
viewModelScope.launch {
    userService.hideLesson(
        groupsCode = "mb-MBB_4",
        title = "Mathe"
    )
}
```

## 5. User macht alles wieder sichtbar

```kotlin
viewModelScope.launch {
    userService.showLessonById(lessonId)
}
```

oder

```kotlin
viewModelScope.launch {
    userService.showLesson(
        groupsCode = "mb-MBB_4",
        title = "Mathe"
    )
}
```

## Fehler- und Offlineverhalten

### Offline beim normalen App-Start

Wenn Room schon Daten hat, funktioniert die App weiter.
Es wird nur lokal gelesen.

### Offline beim Update

`updateJsonIfNeeded()` fängt Netzfehler ab.
Wenn bereits lokale Daten existieren, bleiben diese aktiv.

### Offline beim allerersten Start ohne lokale Daten

Dann kann `initialize()` nicht erfolgreich laden, weil weder Room noch Netz Daten liefern.
Diesen Fall muss die UI sauber behandeln.

Empfehlung:

- Fehlerzustand im ViewModel abbilden
- Retry-Button anbieten

## Wichtige technische Hinweise

## 1. Vor Getter-Nutzung initialisieren

Vor allem diese Methoden setzen voraus, dass vorher geladen wurde:

- `getAllLessons()`
- `getAllEvents()`
- `getAllCalenderDays()`
- `getLessonsByGroupsCode(...)`
- `getLessonById(...)`
- `getLessonsByTitleAndGroupsCode(...)`

Deshalb immer zuerst:

```kotlin
repository.initialize()
```

## 2. Für UI lieber Flows als einmalige Getter

Getter sind gut für direkte Logik.
Für Compose oder Live-UI sind die Flows robuster:

- `repository.calenderDaysFlow`
- `userService.userCalenderDaysFlow()`

## 3. Netzwerklogik nicht in die UI verschieben

Die UI sollte nicht selbst parsen, nicht selbst Room beschreiben und nicht selbst JSON vergleichen.
Diese Arbeit gehört in:

- `DaVinciApi`
- `LessonParser`
- `TimetableRepository`
- `UserTimetableService`

## 4. `CalenderDay` ist absichtlich die Anzeigeform

Für die UI ist `CalenderDay` meistens das Zielmodell.
Nicht `Lesson` oder `Event` direkt als Hauptliste verwenden, wenn die Ansicht tageweise aufgebaut ist.

## 5. Schreibweise `CalenderDay`

Im Projekt heißt die Klasse aktuell `CalenderDay`.
Sprachlich wäre `CalendarDay` korrekter.
Solange keine Umbenennung gewollt ist, muss im Rest des Codes aber der aktuelle Name benutzt werden.

## Sinnvolle nächste Schritte

### 1. Ein echtes `ViewModel` für Stundenplan bauen

Wenn noch nicht vorhanden, sollte ein ViewModel die Startlogik kapseln:

- `initialize()`
- `updateJsonIfNeeded()`
- Exponieren von `userCalenderDaysFlow()`

### 2. UI-Status modellieren

Sinnvoll ist ein UI-State wie:

- `loading`
- `data`
- `error`
- `isOffline`

### 3. Sync-Metadaten für UI lesbar machen

Wenn die UI später anzeigen soll, wann zuletzt synchronisiert wurde, kann `TimetableRepository` noch eine Methode wie `getLastSyncInfo()` bekommen.

### 4. Setup-Flow klar trennen

Eigener Screen für:

- Studiengang wählen
- Setup abschließen

Danach Hauptscreen mit `userCalenderDaysFlow()`.

## Kurzfassung

Für die spätere UI-Anbindung gilt:

- globale Daten kommen aus `TimetableRepository`
- persönliche Auswahl kommt aus `UserSchedulePreferencesStore`
- persönlicher Stundenplan wird von `UserTimetableService` gebaut
- Room ist die lokale Offline-Quelle
- DataStore speichert nur User-Regeln
- für die UI sind `Flow`s die wichtigste Schnittstelle
- die wichtigste Startmethode ist `repository.initialize()`
