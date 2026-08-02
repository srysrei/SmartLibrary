package com.smartlibrary

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.dto.BorrowResponse
import kotlinx.coroutines.launch

/**
 * Shows books to return. Opened either for one specific borrow (EXTRA_BORROW_ID, e.g. from
 * "Return now" on a borrow row) or, when launched from the bottom nav with no extra, for
 * every returnable item across all of the user's approved borrows at once.
 */
class ReturnActivity : AppCompatActivity() {

    private lateinit var borrowRepo: BorrowRepository
    private var borrowId: Long = -1L

    private lateinit var itemsContainer: LinearLayout
    private lateinit var tvSummary: TextView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var progress: ProgressBar

    private val checkboxes = mutableMapOf<Long, CheckBox>()
    private val itemBorrowIds = mutableMapOf<Long, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_return)

        borrowRepo = BorrowRepository(this)
        borrowId = intent.getLongExtra(EXTRA_BORROW_ID, -1L)

        itemsContainer = findViewById(R.id.itemsContainer)
        tvSummary = findViewById(R.id.tvSummary)
        btnConfirm = findViewById(R.id.btnConfirm)
        progress = findViewById(R.id.progress)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        btnConfirm.setOnClickListener { confirm() }

        loadBorrows()
    }

    private fun loadBorrows() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val borrows = if (borrowId != -1L) {
                when (val r = borrowRepo.getBorrow(borrowId)) {
                    is ApiResult.Success -> listOf(r.data)
                    is ApiResult.Error -> {
                        progress.visibility = View.GONE
                        toast(r.message)
                        return@launch
                    }
                }
            } else {
                when (val r = borrowRepo.myBorrows(null)) {
                    is ApiResult.Success -> r.data.filter {
                        it.status.equals("APPROVED", true) || it.status.equals("RETURN", true)
                    }
                    is ApiResult.Error -> {
                        progress.visibility = View.GONE
                        toast(r.message)
                        return@launch
                    }
                }
            }
            progress.visibility = View.GONE
            bind(borrows)
        }
    }

    private fun bind(borrows: List<BorrowResponse>) {
        tvSummary.text = if (borrowId != -1L && borrows.size == 1) {
            val b = borrows[0]
            getString(R.string.return_summary_format, b.id, b.borrowDate ?: "—", b.borrowDay)
        } else {
            getString(R.string.return_summary)
        }

        itemsContainer.removeAllViews()
        checkboxes.clear()
        itemBorrowIds.clear()

        val aggregated = borrowId == -1L
        borrows.forEach { b ->
            b.items.forEach { item ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.bg_card_white)
                    setPadding(26, 26, 26, 26)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 22 }
                }

                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                info.addView(TextView(this).apply {
                    text = item.title ?: getString(R.string.return_default_book, item.bookId)
                    setTextColor(ContextCompat.getColor(context, R.color.ink))
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                info.addView(TextView(this).apply {
                    text = when {
                        item.returned -> getString(R.string.return_already_returned)
                        aggregated -> getString(R.string.return_item_meta_format, item.author ?: "—", b.id)
                        else -> getString(R.string.return_item_author_format, item.author ?: "—")
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.ink_sub))
                    textSize = 11.5f
                })
                row.addView(info)

                if (item.returned) {
                    row.addView(TextView(this).apply {
                        text = getString(R.string.return_returned_check)
                        setTextColor(ContextCompat.getColor(context, R.color.ok))
                        textSize = 12f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                } else {
                    val cb = CheckBox(this)
                    checkboxes[item.id] = cb
                    itemBorrowIds[item.id] = b.id
                    row.addView(cb)
                }

                itemsContainer.addView(row)
            }
        }

        val anyReturnable = borrows.any { b -> b.items.any { !it.returned } }
        btnConfirm.isEnabled = anyReturnable
        btnConfirm.text = getString(if (anyReturnable) R.string.return_confirm else R.string.return_nothing_left)
    }

    private fun confirm() {
        val selectedByBorrow = checkboxes.filter { it.value.isChecked }.keys
            .groupBy { itemBorrowIds.getValue(it) }
        if (selectedByBorrow.isEmpty()) {
            toast(getString(R.string.return_select_one))
            return
        }

        progress.visibility = View.VISIBLE
        btnConfirm.isEnabled = false
        lifecycleScope.launch {
            var lastStatus: String? = null
            var errorMessage: String? = null
            for ((id, itemIds) in selectedByBorrow) {
                when (val r = borrowRepo.returnBooks(id, itemIds)) {
                    is ApiResult.Success -> lastStatus = r.data.status
                    is ApiResult.Error -> errorMessage = r.message
                }
            }
            progress.visibility = View.GONE
            if (errorMessage != null) {
                btnConfirm.isEnabled = true
                toast(errorMessage)
            } else {
                toast(getString(R.string.return_submitted_format, lastStatus.orEmpty()))
                finish()
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_BORROW_ID = "extra_borrow_id"
    }
}
