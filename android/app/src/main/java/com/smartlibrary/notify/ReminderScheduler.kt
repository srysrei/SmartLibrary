package com.smartlibrary.notify

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the [ReminderWorker] that checks for books due soon / overdue and posts a
 * reminder notification.
 *
 * - [setupHome] registers a periodic background check (call from a home screen).
 * - [checkNow] runs the check once immediately.
 * - [cancel] removes any scheduled checks (call on sign-out).
 */
object ReminderScheduler {

    const val CHANNEL_ID = "return_reminders"
    const val NOTIF_ID = 2003

    private const val PERIODIC_WORK = "return_reminder_periodic"
    private const val ONESHOT_WORK = "return_reminder_now"

    private const val NOTIF_PERMISSION_REQUEST = 3001

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Registers a periodic reminder check that runs roughly every 12 hours. */
    fun setupHome(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Runs the reminder check once, right now. */
    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Cancels all scheduled reminder checks. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(PERIODIC_WORK)
            cancelUniqueWork(ONESHOT_WORK)
        }
    }

    /** Requests the POST_NOTIFICATIONS runtime permission on Android 13+ when not yet granted. */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERMISSION_REQUEST,
            )
        }
    }
}
