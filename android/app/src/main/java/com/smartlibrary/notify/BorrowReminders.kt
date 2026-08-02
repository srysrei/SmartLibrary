package com.smartlibrary.notify

import com.smartlibrary.network.dto.BorrowResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Pure helpers that decide which borrows still need to be returned soon.
 *
 * A borrow's due date is [BorrowResponse.borrowDate] + [BorrowResponse.borrowDay] days.
 * Only *active* borrows (APPROVED, or partially RETURNed) with at least one book that has
 * not been returned yet count. Each still-out book is one "unit", so the counts describe
 * books, not borrow records.
 */
object BorrowReminders {

    /** How many days before the due date we start reminding. */
    const val REMIND_AHEAD_DAYS = 2

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Aggregate result: how many still-out books are due soon vs. already overdue. */
    data class Summary(val dueSoon: Int, val overdue: Int) {
        val total: Int get() = dueSoon + overdue
        val hasOverdue: Boolean get() = overdue > 0
        val hasAny: Boolean get() = total > 0
    }

    fun summarize(
        borrows: List<BorrowResponse>,
        remindAheadDays: Int = REMIND_AHEAD_DAYS,
    ): Summary {
        var dueSoon = 0
        var overdue = 0
        borrows.forEach { b ->
            if (!isActive(b)) return@forEach
            val units = unreturnedUnits(b)
            if (units == 0) return@forEach
            val days = daysUntilDue(b) ?: return@forEach
            when {
                days < 0 -> overdue += units
                days <= remindAheadDays -> dueSoon += units
            }
        }
        return Summary(dueSoon, overdue)
    }

    /** APPROVED = fully out; RETURN = partially returned but still has books out. */
    private fun isActive(b: BorrowResponse): Boolean =
        b.status.equals("APPROVED", ignoreCase = true) ||
            b.status.equals("RETURN", ignoreCase = true)

    /** Number of books in this borrow still not returned (falls back to 1 if items missing). */
    private fun unreturnedUnits(b: BorrowResponse): Int =
        if (b.items.isEmpty()) 1 else b.items.count { !it.returned }

    /**
     * Whole days from today until the due date. Negative = overdue. Null if the borrow date
     * is missing or unparseable.
     */
    private fun daysUntilDue(b: BorrowResponse): Long? {
        val start = b.borrowDate?.takeIf { it.isNotBlank() } ?: return null
        val parsed = try {
            dateFmt.parse(start) ?: return null
        } catch (e: Exception) {
            return null
        }
        val due = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_MONTH, b.borrowDay)
        }
        val today = Calendar.getInstance()
        atMidnight(due)
        atMidnight(today)
        return TimeUnit.MILLISECONDS.toDays(due.timeInMillis - today.timeInMillis)
    }

    private fun atMidnight(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
}
