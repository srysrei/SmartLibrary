package com.smartlibrary

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.smartlibrary.data.repository.AuthRepository
import com.smartlibrary.data.repository.ProfileRepository
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.ImageUrls
import com.smartlibrary.network.MultipartFiles
import com.smartlibrary.network.SessionManager
import com.smartlibrary.ui.NavHelper
import com.smartlibrary.ui.UiHelpers
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var profileRepo: ProfileRepository
    private lateinit var authRepo: AuthRepository

    private lateinit var etUsername: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etOldPwd: TextInputEditText
    private lateinit var etNewPwd: TextInputEditText
    private lateinit var progress: ProgressBar

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) uploadAvatar(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        session = SessionManager(this)
        profileRepo = ProfileRepository(this)
        authRepo = AuthRepository(this)

        etUsername = findViewById(R.id.etUsername)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etOldPwd = findViewById(R.id.etOldPwd)
        etNewPwd = findViewById(R.id.etNewPwd)
        progress = findViewById(R.id.progress)

        // Prime from session, then refresh from the API.
        bindHeader(session.fullName, session.email, session.role)
        etUsername.setText(session.username)
        etFullName.setText(session.fullName)
        etEmail.setText(session.email)
        etPhone.setText(session.phone)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }
        findViewById<MaterialButton>(R.id.btnChangePwd).setOnClickListener { changePassword() }
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { logout() }

        val pick = View.OnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        findViewById<View>(R.id.avatarContainer).setOnClickListener(pick)
        findViewById<TextView>(R.id.tvChangePhoto).setOnClickListener(pick)

        findViewById<TextView>(R.id.tvCurrentLanguage).text =
            LocaleManager.displayName(this, LocaleManager.currentLanguage())
        findViewById<View>(R.id.rowLanguage).setOnClickListener { LocaleManager.showPicker(this) }

        findViewById<TextView>(R.id.tvCurrentTheme).text =
            ThemeManager.displayName(this, ThemeManager.currentMode(this))
        findViewById<View>(R.id.rowTheme).setOnClickListener { ThemeManager.showPicker(this) }

        setupNav()
        loadProfile()
    }

    private fun setupNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        if (session.isAdmin()) {
            nav.visibility = View.GONE
        } else {
            nav.inflateMenu(R.menu.menu_user_nav)
            NavHelper.setupUser(this, nav, R.id.nav_profile)
        }
    }

    private fun bindHeader(name: String, email: String, role: String) {
        findViewById<TextView>(R.id.tvName).text = name
        findViewById<TextView>(R.id.tvEmail).text = email
        UiHelpers.bindRoleBadge(findViewById(R.id.tvRole), role)
        bindAvatar()
    }

    /** Shows the uploaded avatar when present, otherwise falls back to the initials. */
    private fun bindAvatar() {
        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        val url = session.imageUrl
        if (url.isNotBlank()) {
            tvAvatar.visibility = View.GONE
            ivAvatar.visibility = View.VISIBLE
            ivAvatar.load(ImageUrls.resolve(url)) { transformations(CircleCropTransformation()) }
        } else {
            ivAvatar.visibility = View.GONE
            tvAvatar.visibility = View.VISIBLE
            tvAvatar.text = session.initials()
        }
    }

    /** Uploads the picked image to POST users/me/image, then refreshes the header avatar. */
    private fun uploadAvatar(uri: Uri) {
        val part = MultipartFiles.imagePart(this, uri)
        if (part == null) {
            toast(getString(R.string.managebook_image_read_failed))
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            when (val r = profileRepo.uploadImage(part)) {
                is ApiResult.Success -> {
                    setLoading(false)
                    session.updateUser(r.data)
                    bindAvatar()
                    toast(getString(R.string.profile_photo_updated))
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(r.message.ifBlank { getString(R.string.managebook_image_upload_failed) })
                }
            }
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            when (val r = profileRepo.getProfile()) {
                is ApiResult.Success -> {
                    session.updateUser(r.data)
                    bindHeader(r.data.fullName, r.data.email, r.data.role)
                    etUsername.setText(r.data.username ?: "")
                    etFullName.setText(r.data.fullName)
                    etEmail.setText(r.data.email)
                    etPhone.setText(r.data.phone ?: "")
                }
                is ApiResult.Error -> { /* keep cached values */ }
            }
        }
    }

    private fun saveProfile() {
        val username = etUsername.text?.toString()?.trim().orEmpty()
        val fullName = etFullName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val phone = etPhone.text?.toString()?.trim().orEmpty()
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            toast(getString(R.string.profile_required))
            return
        }
        // Mirror the server rule: 3-50 chars, letters/digits/dot/underscore/hyphen only.
        if (!username.matches(Regex("^[A-Za-z0-9._-]{3,50}$"))) {
            toast(getString(R.string.username_rule))
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast(getString(R.string.invalid_email))
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            when (val r = profileRepo.updateProfile(username, fullName, email, phone)) {
                is ApiResult.Success -> {
                    setLoading(false)
                    session.updateUser(r.data)
                    bindHeader(r.data.fullName, r.data.email, r.data.role)
                    toast(getString(R.string.profile_updated))
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(r.message)
                }
            }
        }
    }

    private fun changePassword() {
        val old = etOldPwd.text?.toString().orEmpty()
        val new = etNewPwd.text?.toString().orEmpty()
        if (old.isEmpty() || new.isEmpty()) {
            toast(getString(R.string.enter_old_new_password))
            return
        }
        if (new.length < 6) {
            toast(getString(R.string.password_min_length))
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            when (val r = authRepo.changePassword(old, new)) {
                is ApiResult.Success -> {
                    setLoading(false)
                    etOldPwd.text = null
                    etNewPwd.text = null
                    toast(getString(R.string.password_updated))
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    toast(r.message)
                }
            }
        }
    }

    private fun logout() {
        session.clear()
        startActivity(
            Intent(this, LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
