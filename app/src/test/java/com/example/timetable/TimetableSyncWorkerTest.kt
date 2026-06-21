package com.example.timetable

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.data.repository.TimetableRepository
import com.example.timetable.data.services.TimetableSyncWorker
import com.example.timetable.data.services.UserTimetableService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class TimetableSyncWorkerTest {

    @Test
    fun testWorkerExecutionDuringActiveHours() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Initialisiere User Preferences mit einer gültigen Studiengruppe
        val db = TimetableDatabase.getInstance(context)
        val repo = TimetableRepository(context)
        val store = UserSchedulePreferencesStore(context.userSchedulePreferencesDataStore)
        val service = UserTimetableService(repo, store, db)
        service.setGroupsCode("mb-MBB_4")

        // 2. Setze die Systemzeit auf 10:00 Uhr (Arbeitszeit 07:00 - 19:00 Uhr)
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        android.os.SystemClock.setCurrentTimeMillis(calendar.timeInMillis)

        // 3. Simuliere Offline-Status (Netzwerkfehler) durch ungültige Proxy-Einstellungen
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "8008")
        System.setProperty("https.proxyHost", "127.0.0.1")
        System.setProperty("https.proxyPort", "8008")

        try {
            // 4. Worker ausführen
            val worker = TestListenableWorkerBuilder<TimetableSyncWorker>(context).build()
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        } finally {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
        }
    }

    @Test
    fun testWorkerExecutionDuringInactiveHours() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Setze die Systemzeit auf 23:00 Uhr (inaktiv)
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
        }
        android.os.SystemClock.setCurrentTimeMillis(calendar.timeInMillis)

        // 2. Worker ausführen
        val worker = TestListenableWorkerBuilder<TimetableSyncWorker>(context).build()
        val result = worker.doWork()

        // Der Worker sollte sofort ohne Ausführung des Syncs Result.success() zurückliefern
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
