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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartlibrary.adapter.UserAdapter
import com.smartlibrary.data.repository.UserRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import com.smartlibrary.network.dto.UserResponse
import com.smartlibrary.ui.NavHelper
import com.smartlibrary.ui.UiHelpers
import kotlinx.coroutines.launch

class UsersActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var userRepo: UserRepository
    private lateinit var adapter: UserAdapter
    private lateinit var chipContainer: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView

    private var allUsers: List<UserResponse> = emptyList()
    private var searchQuery: String = ""
    private var selectedRole: String? = null

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val roleFilters by lazy {
        listOf(
            getString(R.string.users_filter_all) to null,
            getString(R.string.users_filter_admin) to "ADMIN",
            getString(R.string.users_filter_user) to "USER",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        session = SessionManager(this)
        userRepo = UserRepository(this)

        val onAvatar = View.OnClickListener {
            startActivity(Intent(this@UsersActivity, ProfileActivity::class.java))
        }
        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        tvAvatar.setOnClickListener(onAvatar)
        ivAvatar.setOnClickListener(onAvatar)
        UiHelpers.bindAvatar(tvAvatar, ivAvatar, session.imageUrl, session.initials())
        progress = findViewById(R.id.progress)
        tvEmpty = findViewById(R.id.tvEmpty)
        chipContainer = findViewById(R.id.chipContainer)
        etSearch = findViewById(R.id.etSearch)

        adapter = UserAdapter(emptyList(), session.userId) { user, newRole -> updateRole(user, newRole) }
        findViewById<RecyclerView>(R.id.rvUsers).apply {
            layoutManager = LinearLayoutManager(this@UsersActivity)
            adapter = this@UsersActivity.adapter
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    searchQuery = s?.toString()?.trim().orEmpty()
                    applyFilters()
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.inflateMenu(R.menu.menu_admin_nav)
        NavHelper.setupAdmin(this, nav, R.id.nav_users)

        renderChips()
        loadUsers()
    }

    private fun renderChips() {
        chipContainer.removeAllViews()
        roleFilters.forEach { (label, role) ->
            val selected = role == selectedRole
            val chip = TextView(this).apply {
                text = label
                textSize = 12.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(38, 18, 38, 18)
                setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                setTextColor(ContextCompat.getColor(context, if (selected) R.color.white else R.color.ink_sub))
                setOnClickListener {
                    selectedRole = role
                    renderChips()
                    applyFilters()
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

    private fun loadUsers() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            when (val r = userRepo.listUsers()) {
                is ApiResult.Success -> {
                    progress.visibility = View.GONE
                    allUsers = r.data
                    applyFilters()
                }
                is ApiResult.Error -> {
                    progress.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    toast(r.message)
                }
            }
        }
    }

    private fun applyFilters() {
        val filtered = allUsers.filter { u ->
            (selectedRole == null || u.role.equals(selectedRole, ignoreCase = true)) &&
                (searchQuery.isBlank() || u.fullName.contains(searchQuery, ignoreCase = true))
        }
        adapter.update(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateRole(user: UserResponse, newRole: String) {
        lifecycleScope.launch {
            when (val r = userRepo.updateRole(user.id, newRole)) {
                is ApiResult.Success -> {
                    toast(getString(R.string.users_role_updated, r.data.fullName, r.data.role))
                    loadUsers()
                }
                is ApiResult.Error -> {
                    toast(r.message)
                    loadUsers() // revert switch to server truth
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}