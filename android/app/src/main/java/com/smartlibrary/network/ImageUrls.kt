package com.smartlibrary.network

import com.smartlibrary.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Resolves a book cover [imageUrl] to something an image loader can fetch.
 *
 * The API stores covers as host-relative paths like `/uploads/books/<uuid>.jpg`, served off the
 * host root (outside `/api/`). Resolving them against the API base URL yields the absolute URL,
 * e.g. `http://10.0.2.2:8087/uploads/books/<uuid>.jpg`. Already-absolute `http(s)` values and
 * local `content://` / `file://` picks are returned unchanged.
 */
object ImageUrls {

    fun resolve(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        if (url.isEmpty()) return null
        val lower = url.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("content://") || lower.startsWith("file://")
        ) {
            return url
        }
        val base = BuildConfig.API_BASE_URL.toHttpUrlOrNull() ?: return url
        return base.resolve(url)?.toString() ?: url
    }
}
