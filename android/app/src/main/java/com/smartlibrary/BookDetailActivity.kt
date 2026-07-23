package com.smartlibrary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.button.MaterialButton
import com.smartlibrary.data.repository.BookRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.ImageUrls
import com.smartlibrary.network.SessionManager
import com.smartlibrary.network.dto.BookResponse
import kotlinx.coroutines.launch

class BookDetailActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var bookRepo: BookRepository
    private var bookId: Long = -1L
    private var book: BookResponse? = null

    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        session = SessionManager(this)
        bookRepo = BookRepository(this)
        bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)

        progress = findViewById(R.id.progress)
        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }

        val isAdmin = session.isAdmin()
        findViewById<MaterialButton>(R.id.btnBorrow).visibility = if (isAdmin) View.GONE else View.VISIBLE
        findViewById<LinearLayout>(R.id.adminActions).visibility = if (isAdmin) View.VISIBLE else View.GONE

        findViewById<MaterialButton>(R.id.btnBorrow).setOnClickListener { borrow() }
        findViewById<MaterialButton>(R.id.btnEdit).setOnClickListener {
            startActivity(
                Intent(this, ManageBookActivity::class.java)
                    .putExtra(ManageBookActivity.EXTRA_BOOK_ID, bookId)
            )
        }
        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener { confirmDelete() }
    }

    override fun onResume() {
        super.onResume()
        loadBook()
    }

    private fun loadBook() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = bookRepo.getBook(bookId)) {
                is ApiResult.Success -> {
                    progress.visibility = View.GONE
                    book = r.data
                    bind(r.data)
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun bind(b: BookResponse) {
        findViewById<TextView>(R.id.tvTitle).text = b.title
        findViewById<TextView>(R.id.tvAuthor).text = getString(R.string.by_author, b.author)
        findViewById<TextView>(R.id.tvCategoryPill).text = getString(R.string.bookdetail_category_pill_value, b.categoryName ?: "—")
        findViewById<TextView>(R.id.tvQty).text = b.qty.toString()
        findViewById<TextView>(R.id.tvAvailable).text = b.availableQty.toString()
        findViewById<TextView>(R.id.tvDescription).text = b.description ?: getString(R.string.bookdetail_no_description)

        findViewById<ImageView>(R.id.ivCover).apply {
            if (!b.imageUrl.isNullOrBlank()) load(ImageUrls.resolve(b.imageUrl)) { crossfade(true); error(R.drawable.ic_book) }
        }

        val btnBorrow = findViewById<MaterialButton>(R.id.btnBorrow)
        if (b.availableQty <= 0 && !session.isAdmin()) {
            btnBorrow.isEnabled = false
            btnBorrow.text = getString(R.string.bookdetail_out_of_stock)
        }
    }

    private fun borrow() {
        startActivity(
            Intent(this, BorrowActivity::class.java)
                .putExtra(BorrowActivity.EXTRA_BOOK_ID, bookId)
                .putExtra(BorrowActivity.EXTRA_BOOK_TITLE, book?.title)
                .putExtra(BorrowActivity.EXTRA_BOOK_AUTHOR, book?.author)
        )
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.bookdetail_delete_title))
            .setMessage(getString(R.string.bookdetail_delete_message, book?.title ?: getString(R.string.bookdetail_delete_fallback)))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> doDelete() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun doDelete() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = bookRepo.deleteBook(bookId)) {
                is ApiResult.Success -> {
                    toast(getString(R.string.bookdetail_deleted))
                    finish()
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(r.message)
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
    }
}
