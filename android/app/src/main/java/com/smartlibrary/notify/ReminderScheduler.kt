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
 * Schedules (and cancels) the recurring book-return reminder, plus a one-shot "check right now"
 * used when a home screen opens so reminders surface promptly instead of only on the next cycle.
 */
object ReminderScheduler {

    private const val PERIODIC_WORK = "return_reminder_periodic"
    private const val ONE_SHOT_WORK = "return_reminder_now"
    private const val PERMISSION_REQUEST_CODE = 4201

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Starts the recurring check. Safe to call repeatedly (keeps the existing schedule). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReturnReminderWorker>(6, TimeUnit.HOURS)
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Runs a single immediate check (dedup'd so opening several screens won't spam it). */
    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReturnReminderWorker>()
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_SHOT_WORK, ExistingWorkPolicy.REPLACE, request,
        )
    }

    /** Stops all reminder work — call on logout. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(PERIODIC_WORK)
            cancelUniqueWork(ONE_SHOT_WORK)
        }
    }

    /**
     * One-time setup for a logged-in home screen (call from onCreate): makes sure the channel
     * exists, asks for the Android 13+ notification permission if needed, and schedules the
     * recurring check. Pair with [checkNow] in onResume so a reminder can appear immediately
     * once the permission is granted.
     */
    fun setupHome(activity: Activity) {
        NotificationHelper.ensureChannel(activity)
        requestPermissionIfNeeded(activity)
        schedule(activity)
    }

    private fun requestPermissionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE,
            )
        }
    }
}
