package com.smartlibrary.notify

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartlibrary.BorrowRequestsActivity
import com.smartlibrary.MyBorrowsActivity
import com.smartlibrary.R
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager

/**
 * Background check that reminds the current account about books that are due soon or overdue.
 *
 * - A logged-in **member** is reminded about their own borrows (`borrows/my`) and tapping the
 *   notification opens the "Return" list.
 * - A logged-in **admin** is reminded that members have books due/overdue (`borrows`) and
 *   tapping the notification opens the borrow-requests screen.
 *
 * Runs periodically (see [ReminderScheduler]) and also on demand when a home screen opens.
 */
class ReturnReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val session = SessionManager(ctx)
        if (!session.isLoggedIn()) return Result.success()

        val repo = BorrowRepository(ctx)
        return if (session.isAdmin()) remindAdmin(ctx, repo) else remindUser(ctx, repo)
    }

    private suspend fun remindUser(ctx: Context, repo: BorrowRepository): Result {
        val summary = when (val r = repo.myBorrows(null)) {
            is ApiResult.Success -> BorrowReminders.summarize(r.data)
            is ApiResult.Error -> return Result.retry()
        }
        if (!summary.hasAny) return Result.success()

        val title = ctx.getString(
            if (summary.hasOverdue) R.string.notif_user_title_overdue
            else R.string.notif_user_title,
        )
        val text = ctx.resources.getQuantityString(
            R.plurals.notif_user_text, summary.total, summary.total,
        )
        val open = Intent(ctx, MyBorrowsActivity::class.java)
            .putExtra(MyBorrowsActivity.EXTRA_FILTER, MyBorrowsActivity.FILTER_RETURNABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        NotificationHelper.show(ctx, NotificationHelper.NOTIFICATION_ID_USER, title, text, open)
        return Result.success()
    }

    private suspend fun remindAdmin(ctx: Context, repo: BorrowRepository): Result {
        val summary = when (val r = repo.allBorrows(null)) {
            is ApiResult.Success -> BorrowReminders.summarize(r.data)
            is ApiResult.Error -> return Result.retry()
        }
        if (!summary.hasAny) return Result.success()

        val title = ctx.getString(
            if (summary.hasOverdue) R.string.notif_admin_title_overdue
            else R.string.notif_admin_title,
        )
        val text = ctx.resources.getQuantityString(
            R.plurals.notif_admin_text, summary.total, summary.total,
        )
        val open = Intent(ctx, BorrowRequestsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        NotificationHelper.show(ctx, NotificationHelper.NOTIFICATION_ID_ADMIN, title, text, open)
        return Result.success()
    }
}
