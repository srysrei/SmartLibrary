package com.smartlibrary

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
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

class ReturnActivity : AppCompatActivity() {

    private lateinit var borrowRepo: BorrowRepository
    private var borrowId: Long = -1L

    private lateinit var itemsContainer: LinearLayout
    private lateinit var tvSummary: TextView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var progress: ProgressBar

    private val checkboxes = mutableMapOf<Long, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_return)

        borrowRepo = BorrowRepository(this)
        borrowId = intent.getLongExtra(EXTRA_BORROW_ID, -1L)

        itemsContainer = findViewById(R.id.itemsContainer)
        tvSummary = findViewById(R.id.tvSummary)
        btnConfirm = findViewById(R.id.btnConfirm)
        progress = findViewById(R.id.progress)

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        btnConfirm.setOnClickListener { confirm() }

        loadBorrow()
    }

    private fun loadBorrow() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = borrowRepo.getBorrow(borrowId)) {
                is ApiResult.Success -> {
                    progress.visibility = View.GONE
                    bind(r.data)
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun bind(b: BorrowResponse) {
        tvSummary.text = getString(R.string.return_summary_format, b.id, b.borrowDate ?: "—", b.borrowDay)
        itemsContainer.removeAllViews()
        checkboxes.clear()

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
                text = if (item.returned) getString(R.string.return_already_returned)
                    else getString(R.string.return_item_author_format, item.author ?: "—")
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
                row.addView(cb)
            }

            itemsContainer.addView(row)
        }

        val anyReturnable = b.items.any { !it.returned }
        btnConfirm.isEnabled = anyReturnable
        if (!anyReturnable) btnConfirm.text = getString(R.string.return_nothing_left)
    }

    private fun confirm() {
        val selected = checkboxes.filter { it.value.isChecked }.keys.toList()
        if (selected.isEmpty()) {
            toast(getString(R.string.return_select_one))
            return
        }

        progress.visibility = View.VISIBLE
        btnConfirm.isEnabled = false
        lifecycleScope.launch {
            when (val r = borrowRepo.returnBooks(borrowId, selected)) {
                is ApiResult.Success -> {
                    toast(getString(R.string.return_submitted_format, r.data.status))
                    finish()
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    btnConfirm.isEnabled = true
                    toast(r.message)
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_BORROW_ID = "extra_borrow_id"
    }
}
