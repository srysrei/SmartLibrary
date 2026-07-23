package com.smartlibrary.ui

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartlibrary.BookListActivity
import com.smartlibrary.BorrowRequestsActivity
import com.smartlibrary.DashboardActivity
import com.smartlibrary.ManageBookActivity
import com.smartlibrary.MyBorrowsActivity
import com.smartlibrary.ProfileActivity
import com.smartlibrary.R
import com.smartlibrary.UsersActivity

/**
 * Wires the bottom navigation bars for the user and admin shells so each main screen can
 * jump to its siblings. The layout supplies the menu; this only handles selection routing.
 */
object NavHelper {

    fun setupUser(activity: Activity, nav: BottomNavigationView, currentId: Int) {
        nav.selectedItemId = currentId
        nav.setOnItemSelectedListener { item ->
            if (item.itemId == currentId) return@setOnItemSelectedListener true
            val intent = when (item.itemId) {
                R.id.nav_catalog -> Intent(activity, BookListActivity::class.java)
                R.id.nav_borrows -> Intent(activity, MyBorrowsActivity::class.java)
                R.id.nav_return -> Intent(activity, MyBorrowsActivity::class.java)
                    .putExtra(MyBorrowsActivity.EXTRA_FILTER, MyBorrowsActivity.FILTER_RETURNABLE)
                R.id.nav_profile -> Intent(activity, ProfileActivity::class.java)
                else -> null
            }
            intent?.let { navigate(activity, it) }
            true
        }
    }

    fun setupAdmin(activity: Activity, nav: BottomNavigationView, currentId: Int) {
        nav.selectedItemId = currentId
        nav.setOnItemSelectedListener { item ->
            if (item.itemId == currentId) return@setOnItemSelectedListener true
            val intent = when (item.itemId) {
                R.id.nav_dashboard -> Intent(activity, DashboardActivity::class.java)
                R.id.nav_requests -> Intent(activity, BorrowRequestsActivity::class.java)
                R.id.nav_books -> Intent(activity, BookListActivity::class.java)
                R.id.nav_users -> Intent(activity, UsersActivity::class.java)
                else -> null
            }
            intent?.let { navigate(activity, it) }
            true
        }
    }

    private fun navigate(activity: Activity, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }
}
