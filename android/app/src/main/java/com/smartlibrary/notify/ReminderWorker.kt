package com.smartlibrary.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartlibrary.BookListActivity
import com.smartlibrary.DashboardActivity
import com.smartlibrary.R
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import com.smartlibrary.network.dto.BorrowResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Checks the signed-in user's (or admin's) active borrows for books that are due within
 * a day or already overdue, and posts a single summary notification when any are found.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override suspend fun doWork(): Result {
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn()) return Result.success()

        val isAdmin = session.isAdmin()
        val repo = BorrowRepository(applicationContext)
        val borrows = when (val r = if (isAdmin) repo.allBorrows(null) else repo.myBorrows(null)) {
            is ApiResult.Success -> r.data
            is ApiResult.Error -> return Result.retry()
        }

        val active = borrows.filter { it.status.equals("APPROVED", true) || it.status.equals("RETURN", true) }
        val today = startOfDay(Calendar.getInstance())
        var dueCount = 0
        var overdue = false

        active.forEach { b ->
            val due = dueDate(b) ?: return@forEach
            val unreturned = b.items.count { !it.returned }
            if (unreturned == 0) return@forEach
            val daysLeft = TimeUnit.MILLISECONDS.toDays(due.timeInMillis - today.timeInMillis)
            if (daysLeft <= 1) {
                dueCount += unreturned
                if (daysLeft < 0) overdue = true
            }
        }

        if (dueCount == 0) {
            NotificationManagerCompat.from(applicationContext).cancel(NOTIF_ID)
        } else {
            notify(isAdmin, dueCount, overdue)
        }
        return Result.success()
    }

    private fun dueDate(b: BorrowResponse): Calendar? {
        val start = b.borrowDate ?: return null
        return try {
            val cal = Calendar.getInstance()
            cal.time = inFmt.parse(start) ?: return null
            cal.add(Calendar.DAY_OF_MONTH, b.borrowDay)
            startOfDay(cal)
        } catch (e: Exception) {
            null
        }
    }

    private fun startOfDay(cal: Calendar): Calendar = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun notify(isAdmin: Boolean, count: Int, overdue: Boolean) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val targetActivity = if (isAdmin) DashboardActivity::class.java else BookListActivity::class.java
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, targetActivity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(
            when {
                overdue && isAdmin -> R.string.notif_admin_title_overdue
                overdue -> R.string.notif_user_title_overdue
                isAdmin -> R.string.notif_admin_title
                else -> R.string.notif_user_title
            }
        )
        val text = context.resources.getQuantityString(
            if (isAdmin) R.plurals.notif_admin_text else R.plurals.notif_user_text,
            count,
            count
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setColor(ContextCompat.getColor(context, R.color.brand_600))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    companion object {
        private const val NOTIF_ID = ReminderScheduler.NOTIF_ID
    }
}