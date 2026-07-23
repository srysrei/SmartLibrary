package com.smartlibrary

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate

/**
 * Per-app UI theme (Light / Dark).
 *
 * Uses [AppCompatDelegate.setDefaultNightMode]; on API 24+ appcompat recreates the visible
 * activities so the whole UI re-renders in the chosen theme. Unlike the per-app language, the
 * night mode is not persisted by the framework, so we store the choice in SharedPreferences and
 * re-apply it from [SmartLibraryApp.onCreate] on every launch.
 */
object ThemeManager {

    const val LIGHT = "light"
    const val DARK = "dark"

    /** Theme keys we support, in display order. */
    private val SUPPORTED = listOf(LIGHT, DARK)

    private const val PREFS = "smartlibrary_prefs"
    private const val KEY_THEME = "theme_mode"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Re-applies the stored theme. Call once from the Application before any activity shows. */
    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(currentMode(context)))
    }

    /** Persists the choice and applies it immediately (appcompat recreates the activities). */
    fun setMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME, mode).apply()
        AppCompatDelegate.setDefaultNightMode(nightModeFor(mode))
    }

    /** The currently selected theme key, defaulting to Light. */
    fun currentMode(context: Context): String =
        prefs(context).getString(KEY_THEME, LIGHT) ?: LIGHT

    private fun nightModeFor(mode: String): Int = when (mode) {
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_NO
    }

    /** Human-readable name for a theme key, shown in the picker and the Profile row. */
    fun displayName(context: Context, mode: String): String = when (mode) {
        DARK -> context.getString(R.string.theme_dark)
        else -> context.getString(R.string.theme_light)
    }

    /** Compact sun/moon glyph for the Login theme chip. */
    fun icon(mode: String): String = if (mode == DARK) "🌙" else "☀️"

    /** Shows a chooser dialog; picking a theme applies it immediately. */
    fun showPicker(context: Context) {
        val current = currentMode(context)
        val names = SUPPORTED.map { displayName(context, it) }.toTypedArray()
        val checked = SUPPORTED.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(context)
            .setTitle(R.string.choose_theme)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                dialog.dismiss()
                val mode = SUPPORTED[which]
                if (mode != current) setMode(context, mode)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
