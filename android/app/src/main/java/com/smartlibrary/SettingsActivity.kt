package com.smartlibrary

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.smartlibrary.data.repository.SettingsRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.SessionManager
import kotlinx.coroutines.launch

/**
 * Admin-only screen to view and update the OTP-sender mailbox (table OTPSENDER) — the email +
 * password the API sends password-reset codes from — plus a test-send to verify the credentials.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var settingsRepo: SettingsRepository

    private lateinit var tvStatus: TextView
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etTestTo: TextInputEditText
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        session = SessionManager(this)
        // Guard: this screen and its endpoints are admin-only.
        if (!session.isAdmin()) {
            toast(getString(R.string.settings_admin_access_required))
            finish()
            return
        }
        settingsRepo = SettingsRepository(this)

        tvStatus = findViewById(R.id.tvStatus)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etTestTo = findViewById(R.id.etTestTo)
        progress = findViewById(R.id.progress)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save() }
        findViewById<MaterialButton>(R.id.btnSendTest).setOnClickListener { sendTest() }

        loadCurrent()
    }

    private fun loadCurrent() {
        setLoading(true)
        lifecycleScope.launch {
            when (val r = settingsRepo.getMailSender()) {
                is ApiResult.Success -> {
                    setLoading(false)
                    if (r.data.configured && !r.data.email.isNullOrBlank()) {
                        etEmail.setText(r.data.email)
                        // Prefill the test recipient with the sender for a quick self-check.
                        etTestTo.setText(r.data.email)
                        tvStatus.text = getString(R.string.settings_current_sender, r.data.email)
                    } else {
                        tvStatus.text = getString(R.string.settings_no_sender_configured)
                    }
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    tvStatus.text = getString(R.string.settings_could_not_load_sender)
                    toast(r.message)
                }
            }
        }
    }

    private fun save() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()
        if (email.isEmpty() || password.isEmpty()) {
            toast(getString(R.string.settings_enter_email_and_password))
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast(getString(R.string.settings_enter_valid_email))
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            when (val r = settingsRepo.updateMailSender(email, password)) {
                is ApiResult.Success -> {
                    setLoading(false)
                    etPassword.text = null // don't keep the secret on screen
                    tvStatus.text = getString(R.string.settings_current_sender, r.data.email)
                    toast(getString(R.string.settings_sender_saved))
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(r.message)
                }
            }
        }
    }

    private fun sendTest() {
        val to = etTestTo.text?.toString()?.trim().orEmpty()
        if (to.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(to).matches()) {
            toast(getString(R.string.settings_enter_valid_recipient))
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            when (val r = settingsRepo.sendTestEmail(to)) {
                is ApiResult.Success -> {
                    setLoading(false)
                    toast(r.data.message ?: getString(R.string.settings_test_email_sent))
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(r.message)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
