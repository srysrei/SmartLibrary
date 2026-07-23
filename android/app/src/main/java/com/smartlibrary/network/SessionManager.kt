package com.smartlibrary.network

import android.content.Context
import com.smartlibrary.network.dto.UserResponse

/**
 * Stores the JWT token and the logged-in user's details in SharedPreferences so the
 * session survives app restarts. Also exposes convenience role checks used for routing.
 */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveSession(token: String, user: UserResponse) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username ?: "")
            .putString(KEY_FULL_NAME, user.fullName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone ?: "")
            .putString(KEY_ROLE, user.role)
            .putString(KEY_IMAGE_URL, user.imageUrl ?: "")
            .apply()
    }

    fun updateUser(user: UserResponse) {
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username ?: "")
            .putString(KEY_FULL_NAME, user.fullName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone ?: "")
            .putString(KEY_ROLE, user.role)
            .putString(KEY_IMAGE_URL, user.imageUrl ?: "")
            .apply()
    }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val userId: Long get() = prefs.getLong(KEY_USER_ID, -1L)
    val username: String get() = prefs.getString(KEY_USERNAME, "") ?: ""
    val fullName: String get() = prefs.getString(KEY_FULL_NAME, "") ?: ""
    val email: String get() = prefs.getString(KEY_EMAIL, "") ?: ""
    val phone: String get() = prefs.getString(KEY_PHONE, "") ?: ""
    val role: String get() = prefs.getString(KEY_ROLE, "USER") ?: "USER"
    val imageUrl: String get() = prefs.getString(KEY_IMAGE_URL, "") ?: ""

    fun isLoggedIn(): Boolean = !token.isNullOrEmpty()
    fun isAdmin(): Boolean = role.equals("ADMIN", ignoreCase = true)

    /** Initials for avatar chips, e.g. "Sokha Chan" -> "SC". */
    fun initials(): String {
        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "smartlibrary_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_ROLE = "role"
        private const val KEY_IMAGE_URL = "image_url"
    }
}
