package com.smartlibrary.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smartlibrary.R
import com.smartlibrary.network.dto.BorrowResponse
import com.smartlibrary.ui.UiHelpers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Renders a borrow request row. When [showReturn] is set and the borrow is returnable
 * (APPROVED or partially RETURNed), a "Return now" button is shown wired to [onReturn].
 */
class BorrowAdapter(
    private var items: List<BorrowResponse>,
    private val showReturn: Boolean,
    private val onReturn: (BorrowResponse) -> Unit,
) : RecyclerView.Adapter<BorrowAdapter.VH>() {

    private val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val outFmt = SimpleDateFormat("dd MMM", Locale.US)

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnReturn: MaterialButton = view.findViewById(R.id.btnReturn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_borrow, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b = items[position]

        holder.tvTitle.text = when {
            b.items.size == 1 -> b.items[0].title ?: "1 book"
            b.items.isNotEmpty() -> "${b.items.size} books"
            else -> "Borrow"
        }
        holder.tvMeta.text = "BO #${b.id} · ${dateRange(b.borrowDate, b.borrowDay)}"
        UiHelpers.bindStatusChip(holder.tvStatus, b.status)
        holder.tvSubtitle.text = subtitleFor(b)

        val returnable = showReturn && (b.status.equals("APPROVED", true) || b.status.equals("RETURN", true))
        holder.btnReturn.visibility = if (returnable) View.VISIBLE else View.GONE
        holder.btnReturn.setOnClickListener { onReturn(b) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<BorrowResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun subtitleFor(b: BorrowResponse): String = when (b.status.uppercase()) {
        "PENDING" -> "⏳ Waiting for admin approval"
        "APPROVED" -> "✅ Approved — enjoy your books"
        "REJECT" -> "❌ Rejected by admin"
        "REVERSE" -> "🔁 Approval reversed"
        "RETURN" -> "↩️ Partially returned"
        "RETURNALL" -> "✅ Fully returned"
        else -> ""
    }

    private fun dateRange(start: String?, day: Int): String {
        if (start.isNullOrBlank()) return "—"
        return try {
            val cal = Calendar.getInstance()
            cal.time = inFmt.parse(start)!!
            val from = outFmt.format(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, day)
            val to = outFmt.format(cal.time)
            "$from → $to"
        } catch (e: Exception) {
            start
        }
    }
}
