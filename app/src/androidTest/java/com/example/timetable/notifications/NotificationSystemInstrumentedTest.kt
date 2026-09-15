package com.example.timetable.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class NotificationSystemInstrumentedTest {
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val CHANNEL_ID = "timetable_alerts"
        private const val CHANNEL_NAME = "Stundenplan-Änderungen"
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun testNotificationChannelCreation_hasCorrectImportanceAndId() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Benachrichtigungen über Ausfälle oder Änderungen"
        }
        notificationManager.createNotificationChannel(channel)

        val retrievedChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        assertNotNull("Notification channel must be created", retrievedChannel)
        assertEquals(CHANNEL_ID, retrievedChannel.id)
        assertEquals(CHANNEL_NAME, retrievedChannel.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, retrievedChannel.importance)
    }

    @Test
    fun testNotificationPosting_verifiesActiveNotifications() {
        val notificationTitle = "Raumänderung: Mathematik XIV"
        val notificationText = "Raumänderung von Dumbledores Büro zum Raum der Wünsche."
        //  ensure channel exists first
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // create notification
        val notificationId = 1001
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // trigger notification
        notificationManager.notify(notificationId, notification)

        // retrieve active notifications
        val activeNotifications = notificationManager.activeNotifications
        val postedNotification = activeNotifications.firstOrNull { it.id == notificationId }

        // eval
        assertNotNull("Notification should be posted to NotificationManager", postedNotification)
        assertEquals(
            notificationTitle,
            postedNotification?.notification?.extras?.getString(
                NotificationCompat.EXTRA_TITLE
            )
        )
        assertEquals(
            notificationText,
            postedNotification?.notification?.extras?.getString(
                NotificationCompat.EXTRA_TEXT
            )
        )

        //cleanup
        notificationManager.cancel(notificationId)
    }

    @Test
    fun testPostNotificationPermission_declaredInManifest() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

        assertTrue(
            "POST_NOTIFICATIONS permission must be declared in AndroidManifest",
            requestedPermissions.contains(Manifest.permission.POST_NOTIFICATIONS)
        )
    }
}