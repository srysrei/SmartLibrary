package com.smartlibrary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.smartlibrary.adapter.BorrowAdapter
import com.smartlibrary.data.repository.BorrowRepository
import com.smartlibrary.data.repository.DashboardRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import com.smartlibrary.ui.NavHelper
import com.smartlibrary.ui.UiHelpers
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var dashRepo: DashboardRepository
    private lateinit var borrowRepo: BorrowRepository
    private lateinit var pendingAdapter: BorrowAdapter
    private lateinit var tvNoPending: TextView
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        session = SessionManager(this)
        // Guard: only admins belong here.
        if (!session.isAdmin()) {
            startActivity(Intent(this, BookListActivity::class.java))
            finish()
            return
        }
        ready = true

        dashRepo = DashboardRepository(this)
        borrowRepo = BorrowRepository(this)

        val onAvatar = View.OnClickListener {
            startActivity(Intent(this@DashboardActivity, ProfileActivity::class.java))
        }
        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        tvAvatar.setOnClickListener(onAvatar)
        ivAvatar.setOnClickListener(onAvatar)
        UiHelpers.bindAvatar(tvAvatar, ivAvatar, session.imageUrl, session.initials())
        tvNoPending = findViewById(R.id.tvNoPending)

        pendingAdapter = BorrowAdapter(emptyList(), showReturn = false) {}
        findViewById<RecyclerView>(R.id.rvPending).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = pendingAdapter
        }

        findViewById<MaterialButton>(R.id.btnReviewAll).setOnClickListener {
            startActivity(Intent(this, BorrowRequestsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMailSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.inflateMenu(R.menu.menu_admin_nav)
        NavHelper.setupAdmin(this, nav, R.id.nav_dashboard)
    }

    override fun onResume() {
        super.onResume()
        if (!ready) return
        loadDashboard()
        loadPending()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            when (val r = dashRepo.getDashboard()) {
                is ApiResult.Success -> {
                    findViewById<TextView>(R.id.tvTotalBooks).text = r.data.totalBooks.toString()
                    findViewById<TextView>(R.id.tvTotalUsers).text = r.data.totalUsers.toString()
                    findViewById<TextView>(R.id.tvPending).text = r.data.pendingRequests.toString()
                    findViewById<TextView>(R.id.tvActive).text = r.data.activeBorrows.toString()
                }
                is ApiResult.Error -> { /* leave zeros */ }
            }
        }
    }

    private fun loadPending() {
        lifecycleScope.launch {
            when (val r = borrowRepo.allBorrows("PENDING")) {
                is ApiResult.Success -> {
                    val preview = r.data.take(3)
                    pendingAdapter.update(preview)
                    tvNoPending.visibility = if (preview.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Error -> { /* ignore */ }
            }
        }
    }
}
