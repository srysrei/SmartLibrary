package com.smartlibrary

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.network.ApiResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BorrowActivity : AppCompatActivity() {

    private lateinit var borrowRepo: BorrowRepository
    private var bookId: Long = -1L

    private val calendar = Calendar.getInstance()
    private var days = 14

    private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    private lateinit var tvDate: TextView
    private lateinit var tvDays: TextView
    private lateinit var tvReturnBy: TextView
    private lateinit var etNote: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_borrow)

        borrowRepo = BorrowRepository(this)
        bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)

        findViewById<TextView>(R.id.tvBookTitle).text =
            intent.getStringExtra(EXTRA_BOOK_TITLE) ?: getString(R.string.borrow_default_book)
        findViewById<TextView>(R.id.tvBookAuthor).text =
            getString(R.string.by_author, intent.getStringExtra(EXTRA_BOOK_AUTHOR) ?: getString(R.string.borrow_default_author))

        tvDate = findViewById(R.id.tvDate)
        tvDays = findViewById(R.id.tvDays)
        tvReturnBy = findViewById(R.id.tvReturnBy)
        etNote = findViewById(R.id.etNote)
        btnSubmit = findViewById(R.id.btnSubmit)
        progress = findViewById(R.id.progress)

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.rowDate).setOnClickListener { pickDate() }
        findViewById<TextView>(R.id.btnMinus).setOnClickListener { changeDays(-1) }
        findViewById<TextView>(R.id.btnPlus).setOnClickListener { changeDays(1) }
        btnSubmit.setOnClickListener { submit() }

        refresh()
    }

    private fun pickDate() {
        DatePickerDialog(
            this,
            { _, y, m, d ->
                calendar.set(y, m, d)
                refresh()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun changeDays(delta: Int) {
        days = (days + delta).coerceIn(1, 90)
        refresh()
    }

    private fun refresh() {
        tvDate.text = displayFormat.format(calendar.time)
        tvDays.text = getString(R.string.borrow_days_format, days)
        val returnCal = calendar.clone() as Calendar
        returnCal.add(Calendar.DAY_OF_MONTH, days)
        tvReturnBy.text = getString(R.string.borrow_return_by_format, displayFormat.format(returnCal.time))
    }

    private fun submit() {
        val date = apiFormat.format(calendar.time)
        val note = etNote.text?.toString()?.trim().orEmpty()

        setLoading(true)
        lifecycleScope.launch {
            when (val r = borrowRepo.createBorrow(listOf(bookId), date, days, note.ifBlank { null })) {
                is ApiResult.Success -> {
                    toast(getString(R.string.borrow_request_submitted))
                    finish()
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(r.message)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !loading
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
        const val EXTRA_BOOK_AUTHOR = "extra_book_author"
    }
}
