# Timetable Data Guide

## Ziel dieses Dokuments

Dieses Dokument beschreibt die aktuelle Datenarchitektur der App.
Es ist für Entwickler gedacht, die später UI, ViewModel, Worker oder weitere Features mit dem
Datenlayer verbinden.

Der Fokus liegt auf diesen Fragen:

- Woher kommen die Stundenplandaten?
- Wo werden sie gespeichert?
- Welche Klasse ist für was zuständig?
- Welche Methoden und Flows sollen von UI oder ViewModel benutzt werden?
- Wie wird aus globalen Daten der persönliche Stundenplan eines Users gebaut?
- Welche aktuellen Grenzen oder Risiken gibt es im Modell?

## Architektur auf einen Blick

Der aktuelle Datenfluss ist bewusst getrennt:

1. `DaVinciApi`
   Lädt die aktuelle JSON vom Server.

2. `LessonParser`
   Parsed die JSON in App-Modelle wie `Lesson` und `Event`.

3. `TimetableRepository`
   Ist die zentrale Datenquelle für alle globalen Stundenplandaten.
   Diese Klasse lädt aus Room, aktualisiert bei Bedarf aus dem Netz und stellt Listen, Flows und
   Sync-Status bereit.

4. `UserSchedulePreferencesStore`
   Speichert user-spezifische Regeln in DataStore.

5. `UserTimetableService`
   Kombiniert globale Repository-Daten mit den User-Preferences und baut daraus den persönlichen
   Stundenplan.

6. `CalenderDayMapper`
   Baut aus `Lesson` und `Event` eine kalenderfreundliche Liste von `CalenderDay`.

## Wichtige Grundidee

Es gibt zwei Datenebenen:

### 1. Globale Stundenplandaten

Das sind alle `Lesson`- und `Event`-Einträge aus DaVinci.
Diese Daten gelten für alle User und liegen lokal in Room.

### 2. User-spezifische Auswahl

Das sind nur persönliche Einstellungen und Regeln:

- welcher primäre `groupsCode` gewählt wurde
- welche `extraLessons` aufgenommen werden
- welche `hiddenLessons` ausgeblendet werden
- ob das Setup schon abgeschlossen wurde

Diese Daten liegen in DataStore.

Wichtig:

Der persönliche Stundenplan wird nicht als eigene Tabelle gespeichert.
Er wird immer aus globalen Daten plus User-Regeln berechnet.

## Datenmodelle

### `Lesson`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/Lessons.kt`

`Lesson` repräsentiert eine konkrete Lehrveranstaltung wie Vorlesung, Übung oder Labor mit einer
festen Uhrzeit an einem bestimmten Datum.
Eine `Lesson` findet in einem bestimmten Raum mit Dozenten statt und ist einem oder mehreren
`groupsCode`s zugeordnet.

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

`id` wird im Parser deterministisch aus den Lesson-Daten erzeugt.
Damit kann eine konkrete einzelne Lesson über App-Starts hinweg wiedergefunden werden.

Aktuelle Einschränkung:

Wenn sich für eine Lesson relevante Inhalte wie Raum oder Dozent ändern, ändert sich auch die
generierte `id`.
Dadurch können gespeicherte User-Regeln, die exakt auf diese `lessonId` zeigen, nach einem Update
nicht mehr greifen.
Dieser Punkt ist bekannt und aktuell akzeptiert.
Wenn das später fachlich problematisch wird, muss die User-Regel-Speicherung enger an die Datenbank
gekoppelt werden.

### `Event`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/Events.kt`

`Event` repräsentiert globale Termine wie Ferien, Feiertage oder Prüfungsphasen, die für die gesamte
Hochschule gelten.
Ein `Event` hat ein Start- und Enddatum, aber keine feste Uhrzeit, keinen Raum und keinen
Studiengruppenbezug.

Wichtige Felder:

- `id`
- `title`
- `startDate`
- `endDate`
- `category`

Events können mehrere Tage umfassen.
Die Verteilung auf einzelne Kalendertage macht später `CalenderDayMapper`.

### `groupsCode`

Ein `groupsCode` wie `eti-SKIB 4` oder `mb-SPB_4` repräsentiert typischerweise ein Semester eines
Studiengangs.

Beispiel `eti-SKIB 4`:

- Fakultät: Elektrotechnik und Informatik
- Studiengang: Softwareentwicklung und künstliche Intelligenz
- Semester: 4

Im Setup-Prozess wählt der Nutzer seinen primären `groupsCode`.
Da Vorlesungen häufig von mehreren Studiengängen geteilt werden, besitzt jede `Lesson` ein Set von
`groupsCode`s.

### `CalenderDay`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/CalenderDay.kt`

`CalenderDay` ist die UI-freundliche Tagesansicht.

Felder:

- `date`
- `lessons`
- `events`

Bei regulärer Nutzung einer Tages- oder Wochenansicht sollte die UI mit `List<CalenderDay>`
arbeiten.

Typische Fälle für die direkte Nutzung von `Lesson` oder `Event` statt `CalenderDay` sind:

- Detail-Screen einer `Lesson`
- Suche nach `Lesson`s für `extraLessons`
- Filter- oder Auswahlmasken
- spätere Notification-Logik

### `extraLessons`

`extraLessons` sind `Lesson`s, die der Nutzer zusätzlich in seinem Plan anzeigen möchte, obwohl sie
nicht zu seinem primären `groupsCode` gehören.

Technisch werden diese nicht als komplette `Lesson` gespeichert, sondern als `LessonSelection`.

### `hiddenLessons`

`hiddenLessons` sind Regeln für `Lesson`s, die der Nutzer vom Plan ausblenden möchte.

Technisch werden diese nicht als komplette `Lesson` gespeichert, sondern als `HiddenLessonRule`.

### `UserSchedulePreferences`

Datei: `app/src/main/java/com/example/timetable/data/datenmodell/UserSchedulePreferences.kt`

Felder:

- `isSetupComplete`
- `groupsCode`
- `extraLessons`
- `hiddenLessons`

Zusätzliche Modelle:

- `LessonSelection` - Ermöglicht die Auswahl einzelner Termine (über `lessonId`) oder ganzer Kurse /
  Module (über `title` und `groupsCode`).
- `HiddenLessonRule` - Definiert Ausblendungsregeln. Unterstützt das Ausblenden einzelner Termine (
  über `lessonId`), ganzer Kurse / Module (über `title`) oder ganzer Studiengänge (über
  `groupsCode`).

Diese Typen beschreiben keine kompletten Stundenplandaten, sondern nur User-Regeln.
Das hält Preferences klein und unabhängig von der globalen Datenmenge.

## Zuständigkeiten der Klassen

## `DaVinciApi`

Datei: `app/src/main/java/com/example/timetable/data/services/DaVinciApi.kt`

Zweck:

- lädt die DaVinci-JSON aus dem Netz
- berechnet Hash und Größe der Antwort
- parsed die grobe Root-JSON in `DaVinciResponse`
- läuft vollständig auf `Dispatchers.IO`

Wichtige Methoden:

- `download()`
- `downloadSnapshot()`

`downloadSnapshot()` liefert:

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
- bei strukturell defekter API-Antwort fail-fast abbrechen

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

Außerdem gilt:

Wenn ein Feld wie `roomCodes` oder `classCodes` in der API nicht als JSON-Array geliefert wird,
bricht der Parser bewusst mit einer Exception ab.
Das ist absichtlich so, damit kaputte Serverdaten nicht still downstream zu schwer auffindbaren
Fehlern führen.

## `CalenderDayMapper`

Datei: `app/src/main/java/com/example/timetable/data/services/CalenderDayMapper.kt`

Zweck:

- Lessons nach Datum gruppieren
- Lessons pro Tag nach Startzeit sortieren
- Events auf alle Tage zwischen `startDate` und `endDate` verteilen
- daraus `List<CalenderDay>` bauen

Wichtige Methode:

- `build(lessons, events)`

Die UI sollte für Kalenderansichten meistens das Ergebnis dieser Methode anzeigen.

## `TimetableRepository`

Datei: `app/src/main/java/com/example/timetable/data/TimetableRepository.kt`

Dies ist die zentrale globale Datenklasse.

Zweck:

- lädt Stundenplandaten aus Room
- lädt neue Daten aus dem Netz
- aktualisiert Room bei Änderungen
- hält den zuletzt geladenen Zustand zusätzlich im Speicher
- stellt Flows und einen Sync-Status für UI oder Service bereit

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

### Sync-State

Das Repository stellt zusätzlich einen `StateFlow` bereit:

- `syncState: StateFlow<RepositorySyncState>`

Mögliche Zustände:

- `Idle`
- `LoadingLocal`
- `Syncing`
- `Ready`
- `Error`

Das UI oder ViewModel sollte diesen State beobachten, um differenziert auf folgende Fälle reagieren
zu können:

- lokale Daten werden geladen
- Netzsync läuft
- letzter lokaler Stand ist aktiv
- erster Start ohne Internet ist fehlgeschlagen

### Wichtige Methoden

#### `initialize()`

Verwendung:

- App-Start
- erster Einstieg ins Repository

Verhalten:

- wenn Daten in Room vorhanden sind, werden sie aus Room gelesen
- wenn Room leer ist, wird aus dem Netz geladen und anschließend in Room gespeichert

Dies ist die normale Startmethode.

#### `loadFromDatabase()`

Verwendung:

- wenn explizit nur der lokale DB-Stand neu in den Speicher geladen werden soll

Verhalten:

- liest nur aus Room
- macht keinen Netzaufruf
- gibt `null` zurück, wenn die DB leer ist

Hinweis:

Die ältere Methode `loadFromCache()` existiert nur noch als abwärtskompatibler Alias und sollte
nicht mehr neu verwendet werden.

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
- späterer Background-Worker

Verhalten:

- lädt neues JSON vom Server
- vergleicht `jsonHash` mit gespeicherten Metadaten
- ersetzt Room nur, wenn sich die Daten wirklich geändert haben
- bei Offline-Fehler und vorhandenen lokalen Daten bleibt der letzte lokale Stand erhalten
- bei Offline-Fehler ohne lokale Daten wird die Exception weitergegeben und zusätzlich
  `syncState = Error` gesetzt

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
Darum muss vor der ersten Benutzung mindestens einmal erfolgreich geladen worden sein.
Normalerweise bedeutet das: zuerst `initialize()` aufrufen.

### Flows aus dem Repository

- `lessonsFlow`
- `eventsFlow`
- `calenderDaysFlow`
- `syncState`

Für reaktive UI-Anbindung sollten diese Flows bevorzugt werden.

Hinweis zur Architektur:

Der aktuelle Stand ist bewusst hybrid:

- Room ist die persistente Quelle für globale Daten
- die Repository-Getter spiegeln den zuletzt geladenen In-Memory-Zustand

Das ist funktional in Ordnung, aber noch nicht die strengste mögliche
Single-Source-of-Truth-Variante.
Wenn später Notification- oder Worker-Logik umfangreicher wird, sollte geprüft werden, ob bestimmte
Getter direkt DAO-basiert als `suspend`-Abfragen angeboten werden sollen.

## `UserSchedulePreferencesStore`

Datei: `app/src/main/java/com/example/timetable/data/UserSchedulePreferencesStore.kt`

Zweck:

- speichert User-Einstellungen in DataStore
- liefert einen `Flow<UserSchedulePreferences>`
- fängt beschädigte JSON-Regeln defensiv ab

Wichtige API:

- `preferencesFlow`
- `load()`
- `save(...)`
- `update(...)`
- `clear()`

Wichtig:

Wenn der JSON-Inhalt von `extraLessons` oder `hiddenLessons` beschädigt ist, gibt der Store
nicht-crashend leere Regeln zurück.
Das verhindert, dass eine korrupte DataStore-Value sofort den gesamten Ladepfad blockiert.

## `UserTimetableService`

Datei: `app/src/main/java/com/example/timetable/data/services/UserTimetableService.kt`

Dies ist die wichtigste Klasse für den persönlichen Stundenplan.

Zweck:

- liest globale Stundenplandaten aus `TimetableRepository`
- liest User-Regeln aus `UserSchedulePreferencesStore`
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

### Lessons ausblenden oder wieder anzeigen

- `hideLesson(groupsCode, title)`
- `hideLessonById(lessonId)`
- `showLesson(groupsCode, title)`
- `showLessonById(lessonId)`
- `showAllLessonsByTitle(title)`

### User-Stundenplan bauen

- `buildUserLessons()`
- `buildUserCalenderDays()`

Diese Methoden sind gut für direkte `suspend`-Aufrufe.
Für reaktive UI ist aber `userLessonsFlow()` oder `userCalenderDaysFlow()` meist besser.

## Reihenfolge und Atomarität der User-Regeln

Die Aufbereitung im `UserTimetableService` läuft deterministisch in dieser Reihenfolge:

1. Basis-Lessons über den primären `groupsCode` sammeln
2. `extraLessons` zusätzlich auflösen
3. beide Mengen zusammenführen
4. über `Lesson.id` entdoppeln
5. `hiddenLessons` anwenden
6. Ergebnis nach Datum und Startzeit sortieren

Das bedeutet:

- `hiddenLessons` gewinnen immer gegen `extraLessons`
- wenn dieselbe `Lesson` sowohl extra als auch hidden ist, wird sie am Ende nicht angezeigt

### Was kann konkret ein- oder ausgeblendet werden?

Es gibt zwei Ebenen:

1. Einzeln per `lessonId`
   Damit lässt sich genau ein konkreter Termin ein- oder ausblenden.
   Das ist der richtige Weg für Einzelfälle wie nur eine bestimmte Woche.

2. Fachlich per `title` und optional `groupsCode`
   Damit lassen sich alle passenden Termine einer Lehrveranstaltung ausblenden.

Wichtig für Kurse mit Vorlesung und Labor:

Wenn Vorlesung und Labor denselben `title` teilen, greift eine Titelregel auf beide.
Wenn nur ein einzelner Termin betroffen sein soll, muss mit `lessonId` gearbeitet werden.

## Datenfluss im Detail

### Erstes Laden der App

1. UI oder ViewModel erzeugt `TimetableRepository`
2. `initialize()` wird aufgerufen
3. Repository prüft Room
4. wenn Room leer ist, wird aus dem Netz geladen
5. Parser erzeugt `Lesson` und `Event`
6. Repository speichert alles in Room
7. Repository baut `CalenderDay`
8. UI kann Daten anzeigen

### Erstes Laden der App ohne Internet

1. ViewModel erzeugt `TimetableRepository`
2. `initialize()` wird aufgerufen
3. Repository prüft Room und stellt fest, dass Room leer ist
4. Repository versucht, JSON über `reloadJson()` aus dem Netz zu laden
5. Netzwerkaufruf schlägt fehl (keine Internetverbindung)
6. `syncState` wechselt auf `RepositorySyncState.Error` mit `hasLocalDate = false`
7. Exception wird weitergeworfen und muss vom aufrufenden Scope abgefangen werden
8. UI zeigt einen blockierenden Fehlerbildschirm mit Retry-Option

### Späterer App-Start ohne Internet

1. Repository wird erzeugt
2. `initialize()` wird aufgerufen
3. Room enthält bereits Daten
4. Daten werden lokal geladen
5. UI funktioniert ohne Netz

### Update-Prüfung

1. `updateJsonIfNeeded()` wird aufgerufen
2. `DaVinciApi` lädt neues JSON
3. Repository vergleicht `jsonHash` mit `SyncMetadataEntity`
4. wenn verschieden: neue Daten werden geparst und Room ersetzt
5. wenn gleich: bestehende Daten bleiben erhalten

### Persönlicher Stundenplan eines Users

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
- `UserSchedulePreferencesStore` höchstens für spezielle Setup- oder Debugfälle

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
    val syncState = repository.syncState

    init {
        viewModelScope.launch {
            try {
                repository.initialize()
                repository.updateJsonIfNeeded()
            } catch (exception: Exception) {
                // Optional zusätzliche Fehlerbehandlung.
                // Die UI kann parallel auch repository.syncState beobachten.
            }
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
    val syncState = repository.syncState

    init {
        viewModelScope.launch {
            try {
                repository.initialize()
                repository.updateJsonIfNeeded()
            } catch (exception: Exception) {
                // Optional zusätzliche Fehlerbehandlung.
            }
        }
    }
}
```

### In Compose

```kotlin
@Composable
fun TimetableScreen(viewModel: UserTimetableViewModel) {
    val days by viewModel.userCalenderDays.collectAsState(initial = emptyList())
    val syncState by viewModel.syncState.collectAsState()

    // days rendern
    // syncState für loading/error/offline anzeigen
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

Später für Notifications oder periodische Updates ist dafür ein `PeriodicWorkRequest` sinnvoll.

## Typische Anwendungsfälle

### 1. User wählt seinen Studiengang zum ersten Mal

```kotlin
viewModelScope.launch {
    userService.completeSetup("mb-MBB_4")
}
```

### 2. User fügt ein Fremdmodul hinzu

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

### 3. User blendet nur eine bestimmte Woche aus

```kotlin
viewModelScope.launch {
    userService.hideLessonById(lessonId)
}
```

### 4. User blendet ein Modul eines bestimmten Codes aus

```kotlin
viewModelScope.launch {
    userService.hideLesson(
        groupsCode = "mb-MBB_4",
        title = "Mathe"
    )
}
```

### 5. User macht alles wieder sichtbar

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

### Offline beim normalen App-Start mit bestehenden lokalen Daten

Wenn Room schon Daten hat, funktioniert die App weiter.
Es wird lokal gelesen.

### Offline beim Update

`updateJsonIfNeeded()` fängt Netzfehler ab.
Wenn bereits lokale Daten existieren, bleiben diese aktiv und die Methode liefert `false` zurück.

### Offline beim allerersten Start ohne lokale Daten

Dann kann `initialize()` nicht erfolgreich laden, weil weder Room noch Netz Daten liefern.
In diesem Fall wird ein `Error` in `syncState` gesetzt und die Exception weitergegeben.

Empfehlung für die UI:

- `syncState` beobachten
- zusätzlich den Start-Call im ViewModel in `try-catch` kapseln
- Retry-Button anbieten

## Wichtige technische Hinweise

### 1. Vor Getter-Nutzung initialisieren

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

### 2. Für UI lieber Flows als einmalige Getter

Getter sind gut für direkte Logik.
Für Compose oder andere Live-UI sind Flows robuster:

- `repository.calenderDaysFlow`
- `repository.syncState`
- `userService.userCalenderDaysFlow()`

### 3. Netzwerk- und Parsing-Logik nicht in die UI verschieben

Die UI sollte nicht selbst parsen, nicht selbst Room beschreiben und nicht selbst JSON vergleichen.
Diese Arbeit gehört in:

- `DaVinciApi`
- `LessonParser`
- `TimetableRepository`
- `UserTimetableService`

### 4. `CalenderDay` ist absichtlich die Anzeigeform

Für Kalenderansichten ist `CalenderDay` das primäre Zielmodell.
Direkte `Lesson`- oder `Event`-Listen sind nur für Spezialfälle sinnvoll.

### 5. Schreibweise `CalenderDay`

Im Projekt heißt die Klasse aktuell `CalenderDay`.
Sprachlich wäre `CalendarDay` korrekter.
Solange keine Umbenennung gewollt ist, muss im Rest des Codes aber der aktuelle Name benutzt werden.

## Bewusst nicht umgesetzt

Einige Diskussionspunkte wurden nicht sofort in Code umgebaut, weil sie größere
Architekturänderungen wären:

- komplette Verlagerung aller User-Regeln von DataStore nach Room
- vollständiges Entfernen des Repository-In-Memory-Zustands zugunsten rein DAO-basierter Getter
- vollständige Notification-Architektur über Worker und direkte DB-Abfragen

Diese Punkte sind valide, aber größer als ein gezielter Stabilitäts- oder Qualitäts-Patch.

## Sinnvolle nächste Schritte

### 1. Ein echtes `ViewModel` für Stundenplan bauen

Wenn noch nicht vorhanden, sollte ein ViewModel die Startlogik kapseln:

- `initialize()`
- `updateJsonIfNeeded()`
- Beobachtung von `syncState`
- Exponieren von `userCalenderDaysFlow()`

### 2. UI-Status klar abbilden

Sinnvoll ist ein UI-State wie:

- `loading`
- `data`
- `error`
- `isOfflineFallback`

### 3. Sync-Metadaten für UI lesbar machen

Wenn die UI später anzeigen soll, wann zuletzt synchronisiert wurde, kann das Repository noch eine
Methode oder einen Flow für `SyncMetadataEntity` bekommen.

### 4. User-Regeln fachlich schärfen

Falls die `lessonId`-Instabilität bei Raum- oder Dozentenänderungen zu Problemen führt, muss
entschieden werden, ob User-Regeln künftig relational in Room abgelegt werden sollen.

## Kurzfassung

Für die spätere UI-Anbindung gilt:

- globale Daten kommen aus `TimetableRepository`
- persönliche Auswahl kommt aus `UserSchedulePreferencesStore`
- persönlicher Stundenplan wird von `UserTimetableService` gebaut
- Room ist die lokale Offline-Quelle
- DataStore speichert User-Regeln
- für die UI sind Flows die wichtigste Schnittstelle
- die wichtigste Startmethode ist `repository.initialize()`
- für Fehler- und Offline-Handling sollte `repository.syncState` beobachtet werden
