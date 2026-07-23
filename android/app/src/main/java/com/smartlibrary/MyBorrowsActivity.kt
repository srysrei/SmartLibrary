package com.smartlibrary

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartlibrary.adapter.BorrowAdapter
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import com.smartlibrary.network.dto.BorrowResponse
import com.smartlibrary.ui.NavHelper
import com.smartlibrary.ui.UiHelpers
import kotlinx.coroutines.launch

class MyBorrowsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var borrowRepo: BorrowRepository
    private lateinit var adapter: BorrowAdapter
    private lateinit var chipContainer: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView

    private val filters by lazy {
        listOf(
            getString(R.string.myborrows_filter_to_return) to FILTER_RETURNABLE,
            getString(R.string.myborrows_filter_all) to null,
            getString(R.string.myborrows_filter_pending) to "PENDING",
            getString(R.string.myborrows_filter_approved) to "APPROVED",
            getString(R.string.myborrows_filter_returned) to "RETURNALL",
        )
    }
    private var selectedStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_borrows)

        session = SessionManager(this)
        borrowRepo = BorrowRepository(this)

        selectedStatus = intent.getStringExtra(EXTRA_FILTER)

        val onAvatar = View.OnClickListener {
            startActivity(Intent(this@MyBorrowsActivity, ProfileActivity::class.java))
        }
        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        tvAvatar.setOnClickListener(onAvatar)
        ivAvatar.setOnClickListener(onAvatar)
        UiHelpers.bindAvatar(tvAvatar, ivAvatar, session.imageUrl, session.initials())
        chipContainer = findViewById(R.id.chipContainer)
        progress = findViewById(R.id.progress)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = BorrowAdapter(emptyList(), showReturn = true) { openReturn(it) }
        findViewById<RecyclerView>(R.id.rvBorrows).apply {
            layoutManager = LinearLayoutManager(this@MyBorrowsActivity)
            adapter = this@MyBorrowsActivity.adapter
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.inflateMenu(R.menu.menu_user_nav)
        val current = if (selectedStatus == FILTER_RETURNABLE) R.id.nav_return else R.id.nav_borrows
        NavHelper.setupUser(this, nav, current)

        renderChips()
    }

    override fun onResume() {
        super.onResume()
        loadBorrows()
    }

    private fun renderChips() {
        chipContainer.removeAllViews()
        filters.forEach { (label, status) ->
            val selected = status == selectedStatus
            val chip = TextView(this).apply {
                text = label
                textSize = 12.5f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(38, 18, 38, 18)
                setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                setTextColor(ContextCompat.getColor(context, if (selected) R.color.white else R.color.ink_sub))
                setOnClickListener {
                    selectedStatus = status
                    renderChips()
                    loadBorrows()
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 16
            chip.layoutParams = lp
            chipContainer.addView(chip)
        }
    }

    private fun loadBorrows() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        // "To return" is a client-side view of everything currently returnable
        // (APPROVED or partially RETURNed), since the API filters by a single status.
        val returnable = selectedStatus == FILTER_RETURNABLE
        val statusParam = if (returnable) null else selectedStatus
        lifecycleScope.launch {
            when (val r = borrowRepo.myBorrows(statusParam)) {
                is ApiResult.Success -> {
                    progress.visibility = View.GONE
                    val data = if (returnable) {
                        r.data.filter { it.status.equals("APPROVED", true) || it.status.equals("RETURN", true) }
                    } else {
                        r.data
                    }
                    adapter.update(data)
                    tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    toast(r.message)
                }
            }
        }
    }

    private fun openReturn(b: BorrowResponse) {
        startActivity(
            Intent(this, ReturnActivity::class.java).putExtra(ReturnActivity.EXTRA_BORROW_ID, b.id)
        )
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_FILTER = "extra_filter"
        const val FILTER_RETURNABLE = "RETURNABLE"
    }
}
