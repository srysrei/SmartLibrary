package com.smartlibrary

import android.os.Bundle
import android.os.CountDownTimer
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

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var authRepo: AuthRepository
    private lateinit var etOtp: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnUpdate: MaterialButton
    private lateinit var progress: ProgressBar
    private lateinit var tvTimer: TextView

    private var email: String = ""
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        authRepo = AuthRepository(this)
        email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()

        etOtp = findViewById(R.id.etOtp)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        btnUpdate = findViewById(R.id.btnUpdate)
        progress = findViewById(R.id.progress)
        tvTimer = findViewById(R.id.tvTimer)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // The code is delivered by email only — the field starts empty and the user types it in.
        // Count down the remaining validity so they know how long the code stays usable.
        val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, System.currentTimeMillis() + OTP_TTL_MS)
        startTimer(expiresAt)

        btnUpdate.setOnClickListener { doReset() }
    }

    private fun startTimer(expiresAt: Long) {
        val remaining = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(msLeft: Long) {
                val totalSec = msLeft / 1000
                tvTimer.text = String.format("%02d:%02d", totalSec / 60, totalSec % 60)
            }

            override fun onFinish() {
                tvTimer.text = getString(R.string.reset_expired)
                btnUpdate.isEnabled = false
                toast(getString(R.string.reset_code_expired))
            }
        }.start()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    private fun doReset() {
        val otp = etOtp.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirm = etConfirm.text?.toString().orEmpty()

        if (otp.isEmpty() || password.isEmpty()) {
            toast(getString(R.string.reset_enter_code_and_password))
            return
        }
        if (password.length < 6) {
            toast(getString(R.string.reset_password_min_length))
            return
        }
        if (password != confirm) {
            toast(getString(R.string.reset_passwords_no_match))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val result = authRepo.resetPassword(email, otp, password)) {
                is ApiResult.Success -> {
                    toast(getString(R.string.reset_password_updated))
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
        btnUpdate.isEnabled = !loading
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_EXPIRES_AT = "extra_expires_at"

        /** OTP validity window; mirrors OTP_TTL_MINUTES on the API. */
        const val OTP_TTL_MS = 10 * 60 * 1000L
    }
}
