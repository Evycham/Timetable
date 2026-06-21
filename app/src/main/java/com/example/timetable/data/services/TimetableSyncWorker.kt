package com.example.timetable.data.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.icu.util.Calendar
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timetable.data.local.db.TimetableDatabase
import com.example.timetable.data.local.preferences.UserSchedulePreferencesStore
import com.example.timetable.data.local.preferences.userSchedulePreferencesDataStore
import com.example.timetable.data.repository.TimetableRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds


/**
 * Background-Worker zur periodischen Synchronisierung der Stundenplandaten.
 * Prüft auf Änderungen und löst lokale Systembenachrichtigungen aus.
 */
class TimetableSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    /**
     * Definiert die höchstmögliche Wartezeit, um Anfragen auf dem DaVinci Server zu entzerren.
     */
    companion object {
        private const val MAX_RANDOM_DELAY_MS = 5 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        // time constraint: execution only between 0700 and 1900
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour !in 7..19) {
            return Result.success()
        }

        // add jitter so the backend does not get overloaded
        val randomDelayMS = Random.nextLong(0, MAX_RANDOM_DELAY_MS + 1)
        // suspend random delay duration
        kotlinx.coroutines.delay(randomDelayMS.milliseconds)

        return try {
            val database = TimetableDatabase.getInstance(applicationContext)
            val repository = TimetableRepository(applicationContext)
            val preferencesStore =
                UserSchedulePreferencesStore(applicationContext.userSchedulePreferencesDataStore)
            val userService = UserTimetableService(repository, preferencesStore, database)

            val prefs = userService.getPreferences()
            val userGroupCode = prefs.groupsCode ?: return Result.success()

            // read current state of userLessons
            val oldUserLessons = userService.userLessonsFlow().first()

            // trigger sync
            val hasUpdates = repository.updateJsonIfNeeded()

            // if new data, detect changes and trigger notifications
            if (hasUpdates) {
                val newLessons = repository.lessonsFlow.first()
                val extraLessons = database.userRulesDao().getExtraLessons()
                val hiddenLessons = database.userRulesDao().getHiddenLessons()

                val detector = TimetableAlertDetector()
                val alerts = detector.detectAlerts(
                    oldUserLessons = oldUserLessons,
                    newLessons = newLessons,
                    userGroupsCode = userGroupCode,
                    extraLessons = extraLessons,
                    hiddenLessons = hiddenLessons
                )

                // read notification settings
                val showCancellation = prefs.isCancellationAlertEnabled
                val showRoomChange = prefs.isRoomChangeAlertEnabled

                val notificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // create channel
                val channel = NotificationChannel(
                    "timetable_alerts",
                    "Stundenplan-Änderungen",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Benachrichtigungen über Ausfälle oder Raumänderungen"
                }
                notificationManager.createNotificationChannel(channel)

                // send notifications
                for (alert in alerts) {
                    val isEnabled = when (alert.type) {
                        AlertType.CANCELLATION -> showCancellation
                        AlertType.ROOM_CHANGE -> showRoomChange
                    }

                    if (isEnabled) {
                        val title = when (alert.type) {
                            AlertType.CANCELLATION -> "Vorlesungsausfall: ${alert.lessonTitle}"
                            AlertType.ROOM_CHANGE -> "Raumänderung: ${alert.lessonTitle}"
                        }
                        val builder =
                            NotificationCompat.Builder(applicationContext, "timetable_alerts")
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle(title)
                                .setContentText(alert.detail)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setAutoCancel(true)
                        notificationManager.notify(alert.hashCode(), builder.build())
                    }
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}