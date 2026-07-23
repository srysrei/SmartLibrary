package com.smartlibrary.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.smartlibrary.R
import com.smartlibrary.network.ImageUrls

/**
 * Shared visual helpers so borrow status chips, role badges and stock indicators look
 * consistent across every screen — matching the HTML prototype's colour system.
 */
object UiHelpers {

    /** Background + text colour resource ids for each BORROWS.BO_STS value. */
    fun statusColors(status: String): Pair<Int, Int> = when (status.uppercase()) {
        "PENDING" -> R.color.s_pending_bg to R.color.s_pending_tx
        "APPROVED" -> R.color.s_approved_bg to R.color.s_approved_tx
        "REJECT" -> R.color.s_reject_bg to R.color.s_reject_tx
        "REVERSE" -> R.color.s_reverse_bg to R.color.s_reverse_tx
        "RETURN" -> R.color.s_return_bg to R.color.s_return_tx
        "RETURNALL" -> R.color.s_returnall_bg to R.color.s_returnall_tx
        else -> R.color.line to R.color.ink_sub
    }

    /** Styles a TextView as a rounded status pill for the given status. */
    fun bindStatusChip(chip: TextView, status: String) {
        val (bg, tx) = statusColors(status)
        chip.text = status.uppercase()
        chip.setBackgroundResource(R.drawable.bg_pill)
        chip.backgroundTintList = ContextCompat.getColorStateList(chip.context, bg)
        chip.setTextColor(ContextCompat.getColor(chip.context, tx))
    }

    /** Styles a TextView as a USER / ADMIN role badge. */
    fun bindRoleBadge(badge: TextView, role: String) {
        val admin = role.equals("ADMIN", ignoreCase = true)
        badge.text = role.uppercase()
        badge.setBackgroundResource(R.drawable.bg_pill)
        badge.backgroundTintList = ContextCompat.getColorStateList(
            badge.context, if (admin) R.color.r_admin_bg else R.color.r_user_bg
        )
        badge.setTextColor(
            ContextCompat.getColor(badge.context, if (admin) R.color.r_admin_tx else R.color.r_user_tx)
        )
    }

    /** Stock label + colour based on available copies. */
    fun stockText(available: Int): String = when {
        available <= 0 -> "Out of stock"
        available <= 2 -> "$available left"
        else -> "$available available"
    }

    fun stockColor(context: Context, available: Int): Int = ContextCompat.getColor(
        context,
        when {
            available <= 0 -> R.color.bad
            available <= 2 -> R.color.warn
            else -> R.color.ok
        }
    )

    fun View.visibleIf(condition: Boolean) {
        visibility = if (condition) View.VISIBLE else View.GONE
    }

    /**
     * Binds a header/list avatar: when [imageUrl] is set, shows the circle-cropped photo in
     * [ivAvatar] and hides the initials; otherwise shows [initials] in [tvAvatar]. Mirrors the
     * "photo if present, initials otherwise" rule used everywhere the current user is shown.
     */
    fun bindAvatar(tvAvatar: TextView, ivAvatar: ImageView, imageUrl: String?, initials: String) {
        if (!imageUrl.isNullOrBlank()) {
            tvAvatar.visibility = View.GONE
            ivAvatar.visibility = View.VISIBLE
            ivAvatar.load(ImageUrls.resolve(imageUrl)) { transformations(CircleCropTransformation()) }
        } else {
            ivAvatar.visibility = View.GONE
            tvAvatar.visibility = View.VISIBLE
            tvAvatar.text = initials
        }
    }
}
