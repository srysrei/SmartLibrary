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

class RegisterActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var authRepo: AuthRepository

    private lateinit var etUsername: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        session = SessionManager(this)
        authRepo = AuthRepository(this)

        etUsername = findViewById(R.id.etUsername)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        progress = findViewById(R.id.progress)

        btnRegister.setOnClickListener { doRegister() }
        findViewById<TextView>(R.id.tvSignIn).setOnClickListener { finish() }
    }

    private fun doRegister() {
        val username = etUsername.text?.toString()?.trim().orEmpty()
        val fullName = etFullName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirm = etConfirm.text?.toString().orEmpty()

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            toast(getString(R.string.register_fill_all_fields))
            return
        }
        // Mirror the server rule: 3-50 chars, letters/digits/dot/underscore/hyphen only.
        if (!username.matches(Regex("^[A-Za-z0-9._-]{3,50}$"))) {
            toast(getString(R.string.register_username_rule))
            return
        }
        if (password.length < 6) {
            toast(getString(R.string.register_password_min_length))
            return
        }
        if (password != confirm) {
            toast(getString(R.string.register_passwords_no_match))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val result = authRepo.register(username, fullName, email, phone, password)) {
                is ApiResult.Success -> {
                    session.saveSession(result.data.token, result.data.user)
                    toast(getString(R.string.register_welcome, result.data.user.fullName))
                    startActivity(
                        Intent(this@RegisterActivity, BookListActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                    )
                    finish()
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(result.message)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
