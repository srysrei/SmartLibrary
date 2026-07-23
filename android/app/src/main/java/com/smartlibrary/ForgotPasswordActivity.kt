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
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var authRepo: AuthRepository
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        authRepo = AuthRepository(this)
        etEmail = findViewById(R.id.etEmail)
        btnSend = findViewById(R.id.btnSend)
        progress = findViewById(R.id.progress)

        btnSend.setOnClickListener { doSend() }
        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
    }

    private fun doSend() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        if (email.isEmpty()) {
            toast(getString(R.string.forgot_enter_email))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val result = authRepo.forgotPassword(email)) {
                is ApiResult.Success -> {
                    setLoading(false)
                    toast(getString(R.string.forgot_reset_code_sent))
                    startActivity(
                        Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                            .putExtra(ResetPasswordActivity.EXTRA_EMAIL, email)
                            .putExtra(
                                ResetPasswordActivity.EXTRA_EXPIRES_AT,
                                System.currentTimeMillis() + ResetPasswordActivity.OTP_TTL_MS
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
        btnSend.isEnabled = !loading
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
