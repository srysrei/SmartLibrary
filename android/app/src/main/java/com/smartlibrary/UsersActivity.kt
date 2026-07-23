package com.smartlibrary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView

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

        adapter = UserAdapter(emptyList(), session.userId) { user, newRole -> updateRole(user, newRole) }
        findViewById<RecyclerView>(R.id.rvUsers).apply {
            layoutManager = LinearLayoutManager(this@UsersActivity)
            adapter = this@UsersActivity.adapter
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.inflateMenu(R.menu.menu_admin_nav)
        NavHelper.setupAdmin(this, nav, R.id.nav_users)

        loadUsers()
    }

    private fun loadUsers() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            when (val r = userRepo.listUsers()) {
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
