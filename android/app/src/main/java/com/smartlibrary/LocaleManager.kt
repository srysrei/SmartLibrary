package com.smartlibrary

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app language (English / Khmer).
 *
 * Uses [AppCompatDelegate.setApplicationLocales], the AndroidX per-app-language API. On API 33+ it
 * delegates to the platform; on API 24–32 appcompat backports it. Persistence is automatic thanks
 * to the `autoStoreLocales` metadata declared in the manifest, so the choice survives restarts and
 * every activity is recreated in the new language without any manual plumbing.
 */
object LocaleManager {

    const val ENGLISH = "en"
    const val KHMER = "km"

    /** Language tags we support, in display order. */
    private val SUPPORTED = listOf(ENGLISH, KHMER)

    /** Applies the given BCP-47 language tag and recreates the visible activities. */
    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** The currently active app language tag, defaulting to English when none is set. */
    fun currentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) ENGLISH else locales[0]?.language ?: ENGLISH
    }

    /** Human-readable name for a language tag, shown in the picker and the toggle label. */
    fun displayName(context: Context, tag: String): String = when (tag) {
        KHMER -> context.getString(R.string.lang_khmer)
        else -> context.getString(R.string.lang_english)
    }

    /** Short label (e.g. "EN" / "ខ្មែរ") for a compact toggle chip. */
    fun shortLabel(tag: String): String = when (tag) {
        KHMER -> "ខ្មែរ"
        else -> "EN"
    }

    /**
     * Shows a simple chooser dialog. Selecting a language applies it immediately; the current
     * activity is recreated by appcompat so the whole UI re-renders in the chosen language.
     */
    fun showPicker(context: Context) {
        val current = currentLanguage()
        val names = SUPPORTED.map { displayName(context, it) }.toTypedArray()
        val checked = SUPPORTED.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(context)
            .setTitle(R.string.choose_language)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                dialog.dismiss()
                val tag = SUPPORTED[which]
                if (tag != current) setLanguage(tag)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
