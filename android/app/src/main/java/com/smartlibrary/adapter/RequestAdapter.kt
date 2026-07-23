package com.smartlibrary.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
 * Admin borrow-request row. Shows Approve/Reject for PENDING and Reverse for APPROVED,
 * matching the API's allowed transitions.
 */
class RequestAdapter(
    private var items: List<BorrowResponse>,
    private val onApprove: (BorrowResponse) -> Unit,
    private val onReject: (BorrowResponse) -> Unit,
    private val onReverse: (BorrowResponse) -> Unit,
) : RecyclerView.Adapter<RequestAdapter.VH>() {

    private val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val outFmt = SimpleDateFormat("dd MMM", Locale.US)

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvUser: TextView = view.findViewById(R.id.tvUser)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        val actionsPending: LinearLayout = view.findViewById(R.id.actionsPending)
        val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        val btnReverse: MaterialButton = view.findViewById(R.id.btnReverse)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b = items[position]
        holder.tvTitle.text = when {
            b.items.size == 1 -> b.items[0].title ?: "1 book"
            b.items.isNotEmpty() -> "${b.items.size} books"
            else -> "Borrow"
        }
        holder.tvUser.text = "👤 ${b.userName ?: "User #${b.userId}"} · BO #${b.id}"
        UiHelpers.bindStatusChip(holder.tvStatus, b.status)
        holder.tvMeta.text = "📅 ${dateRange(b.borrowDate, b.borrowDay)} (${b.borrowDay} days) · ${b.items.size} book(s)"

        val isPending = b.status.equals("PENDING", true)
        val isApproved = b.status.equals("APPROVED", true)

        holder.actionsPending.visibility = if (isPending) View.VISIBLE else View.GONE
        holder.btnReverse.visibility = if (isApproved) View.VISIBLE else View.GONE

        holder.btnApprove.setOnClickListener { onApprove(b) }
        holder.btnReject.setOnClickListener { onReject(b) }
        holder.btnReverse.setOnClickListener { onReverse(b) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<BorrowResponse>) {
        items = newItems
        notifyDataSetChanged()
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
