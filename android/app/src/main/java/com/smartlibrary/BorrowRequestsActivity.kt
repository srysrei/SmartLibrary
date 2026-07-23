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
import com.smartlibrary.adapter.RequestAdapter
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import com.smartlibrary.network.dto.BorrowResponse
import com.smartlibrary.ui.NavHelper
import com.smartlibrary.ui.UiHelpers
import kotlinx.coroutines.launch

class BorrowRequestsActivity : AppCompatActivity() {

    private lateinit var borrowRepo: BorrowRepository
    private lateinit var adapter: RequestAdapter
    private lateinit var chipContainer: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView

    private val filters by lazy {
        listOf(
            getString(R.string.requests_filter_pending) to "PENDING",
            getString(R.string.requests_filter_approved) to "APPROVED",
            getString(R.string.requests_filter_rejected) to "REJECT",
            getString(R.string.requests_filter_returned) to "RETURNALL",
            getString(R.string.requests_filter_all) to null,
        )
    }
    private var selectedStatus: String? = "PENDING"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_borrow_requests)

        borrowRepo = BorrowRepository(this)
        val sm = SessionManager(this@BorrowRequestsActivity)
        val onAvatar = View.OnClickListener {
            startActivity(Intent(this@BorrowRequestsActivity, ProfileActivity::class.java))
        }
        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        tvAvatar.setOnClickListener(onAvatar)
        ivAvatar.setOnClickListener(onAvatar)
        UiHelpers.bindAvatar(tvAvatar, ivAvatar, sm.imageUrl, sm.initials())

        chipContainer = findViewById(R.id.chipContainer)
        progress = findViewById(R.id.progress)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = RequestAdapter(
            emptyList(),
            onApprove = { act("approve", it) },
            onReject = { act("reject", it) },
            onReverse = { act("reverse", it) },
        )
        findViewById<RecyclerView>(R.id.rvRequests).apply {
            layoutManager = LinearLayoutManager(this@BorrowRequestsActivity)
            adapter = this@BorrowRequestsActivity.adapter
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.inflateMenu(R.menu.menu_admin_nav)
        NavHelper.setupAdmin(this, nav, R.id.nav_requests)

        renderChips()
        loadRequests()
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
                    loadRequests()
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

    private fun loadRequests() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            when (val r = borrowRepo.allBorrows(selectedStatus)) {
                is ApiResult.Success -> {
                    progress.visibility = View.GONE
                    adapter.update(r.data)
                    tvEmpty.visibility = if (r.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    toast(r.message)
                }
            }
        }
    }

    private fun act(action: String, b: BorrowResponse) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = when (action) {
                "approve" -> borrowRepo.approve(b.id)
                "reject" -> borrowRepo.reject(b.id)
                else -> borrowRepo.reverse(b.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    toast(getString(R.string.requests_status_updated, result.data.status))
                    loadRequests()
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    toast(result.message)
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
