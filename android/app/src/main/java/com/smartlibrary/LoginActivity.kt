package com.smartlibrary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.smartlibrary.data.repository.AuthRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var authRepo: AuthRepository

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = SessionManager(this)
        authRepo = AuthRepository(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progress = findViewById(R.id.progress)

        val tvLanguage = findViewById<TextView>(R.id.tvLanguage)
        tvLanguage.text = "🌐 " + LocaleManager.shortLabel(LocaleManager.currentLanguage())
        tvLanguage.setOnClickListener { LocaleManager.showPicker(this) }

        val tvTheme = findViewById<TextView>(R.id.tvTheme)
        tvTheme.text = ThemeManager.icon(ThemeManager.currentMode(this))
        tvTheme.setOnClickListener { ThemeManager.showPicker(this) }

        btnLogin.setOnClickListener { doLogin() }
        findViewById<TextView>(R.id.tvRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<TextView>(R.id.tvForgot).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun doLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        if (email.isEmpty() || password.isEmpty()) {
            toast(getString(R.string.enter_credentials))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val result = authRepo.login(email, password)) {
                is ApiResult.Success -> {
                    session.saveSession(result.data.token, result.data.user)
                    goHome()
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(result.message)
                }
            }
        }
    }

    private fun goHome() {
        val next = if (session.isAdmin()) DashboardActivity::class.java else BookListActivity::class.java
        startActivity(
            Intent(this, next).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
