package com.smartlibrary

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.smartlibrary.adapter.BookAdapter
import com.smartlibrary.data.repository.BookRepository
import com.smartlibrary.data.repository.CategoryRepository
import com.smartlibrary.data.repository.toResponse
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import com.smartlibrary.network.dto.BookResponse
import com.smartlibrary.ui.NavHelper
import com.smartlibrary.ui.UiHelpers
import kotlinx.coroutines.launch

class BookListActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var bookRepo: BookRepository
    private lateinit var categoryRepo: CategoryRepository

    private lateinit var adapter: BookAdapter
    private lateinit var chipContainer: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText

    private data class Cat(val id: Long?, val name: String)

    private var categories: List<Cat> = emptyList()
    private var selectedCategoryId: Long? = null
    private var searchQuery: String = ""

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_list)

        session = SessionManager(this)
        bookRepo = BookRepository(this)
        categoryRepo = CategoryRepository(this)

        val greetingName = session.fullName.ifBlank { getString(R.string.booklist_greeting_fallback) }
        findViewById<TextView>(R.id.tvGreeting).text = getString(R.string.booklist_greeting, greetingName)
        val onAvatar = View.OnClickListener {
            startActivity(Intent(this@BookListActivity, ProfileActivity::class.java))
        }
        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        tvAvatar.setOnClickListener(onAvatar)
        ivAvatar.setOnClickListener(onAvatar)
        UiHelpers.bindAvatar(tvAvatar, ivAvatar, session.imageUrl, session.initials())

        chipContainer = findViewById(R.id.chipContainer)
        progress = findViewById(R.id.progress)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)

        adapter = BookAdapter(emptyList()) { openBook(it) }
        val rv = findViewById<RecyclerView>(R.id.rvBooks)
        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    searchQuery = s?.toString()?.trim().orEmpty()
                    loadBooks()
                }
                searchHandler.postDelayed(searchRunnable!!, 350)
            }
        })

        setupBottomNav()
        setupFab()
        loadCategories()
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        if (session.isAdmin()) {
            nav.inflateMenu(R.menu.menu_admin_nav)
            NavHelper.setupAdmin(this, nav, R.id.nav_books)
        } else {
            nav.inflateMenu(R.menu.menu_user_nav)
            NavHelper.setupUser(this, nav, R.id.nav_catalog)
        }
    }

    private fun setupFab() {
        val fab = findViewById<FloatingActionButton>(R.id.fabAdd)
        if (session.isAdmin()) {
            fab.visibility = View.VISIBLE
            fab.setOnClickListener { startActivity(Intent(this, ManageBookActivity::class.java)) }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val fromApi = when (val r = categoryRepo.fetchCategories()) {
                is ApiResult.Success -> r.data.map { Cat(it.id, it.name) }
                is ApiResult.Error -> categoryRepo.cachedCategories().map { Cat(it.id, it.name) }
            }
            categories = listOf(Cat(null, getString(R.string.booklist_all))) + fromApi
            renderChips()
        }
    }

    private fun renderChips() {
        chipContainer.removeAllViews()
        categories.forEach { cat ->
            val selected = cat.id == selectedCategoryId
            val chip = TextView(this).apply {
                text = cat.name
                textSize = 12.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(38, 18, 38, 18)
                setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                setTextColor(ContextCompat.getColor(context, if (selected) R.color.white else R.color.ink_sub))
                setOnClickListener {
                    selectedCategoryId = cat.id
                    renderChips()
                    loadBooks()
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 16
            chip.layoutParams = lp
            chipContainer.addView(chip)
        }
    }

    private fun loadBooks() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val books: List<BookResponse> = when (val r = bookRepo.fetchBooks(searchQuery, selectedCategoryId)) {
                is ApiResult.Success -> r.data
                is ApiResult.Error -> bookRepo.cachedBooks(searchQuery, selectedCategoryId).map { it.toResponse() }
            }
            progress.visibility = View.GONE
            adapter.updateBooks(books)
            tvEmpty.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openBook(book: BookResponse) {
        startActivity(
            Intent(this, BookDetailActivity::class.java)
                .putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.id)
        )
    }
}
