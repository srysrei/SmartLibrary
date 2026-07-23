package com.smartlibrary.network

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Builds multipart parts from picked images for the `file`-based upload endpoints. */
object MultipartFiles {

    /**
     * Reads the image at [uri] into a multipart part named [partName] (default `file`, matching the
     * API's `@RequestParam("file")`). Returns null when the image can't be read.
     */
    fun imagePart(context: Context, uri: Uri, partName: String = "file"): MultipartBody.Part? =
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/*"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val ext = mime.substringAfterLast('/', "jpg").take(5)
            val fileName = "upload_${System.currentTimeMillis()}.$ext"
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull(), 0, bytes.size)
            MultipartBody.Part.createFormData(partName, fileName, body)
        }.getOrNull()
}
