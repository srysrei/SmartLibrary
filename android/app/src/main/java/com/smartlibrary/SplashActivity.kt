package com.smartlibrary

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.smartlibrary.network.SessionManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val session = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val next = when {
                !session.isLoggedIn() -> LoginActivity::class.java
                session.isAdmin() -> DashboardActivity::class.java
                else -> BookListActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
        }, 1600)
    }
}
