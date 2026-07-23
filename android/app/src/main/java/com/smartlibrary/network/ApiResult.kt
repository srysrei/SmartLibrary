package com.smartlibrary.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.Response
import java.io.IOException

/** Simple success/error wrapper so screens can handle API calls uniformly. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

/** Shape of the API's error body: {status, error, message, errors?}. */
private data class ApiError(
    val status: Int?,
    val error: String?,
    val message: String?,
    val errors: Map<String, String>?,
)

/**
 * Runs a suspend Retrofit call and maps it to an [ApiResult], extracting the server's
 * error message when the response is unsuccessful and translating network exceptions
 * into a friendly message.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                // All SmartLibrary endpoints return a JSON body on success; a null body
                // means an unexpected empty/204 response. Report it rather than casting.
                ApiResult.Error("Empty response from server", response.code())
            }
        } else {
            ApiResult.Error(parseError(response), response.code())
        }
    } catch (e: IOException) {
        ApiResult.Error("Cannot reach the server. Is the API running and reachable?")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unexpected error")
    }
}

private fun parseError(response: Response<*>): String {
    return try {
        val raw = response.errorBody()?.string()
        if (raw.isNullOrBlank()) {
            defaultMessage(response.code())
        } else {
            val parsed = Gson().fromJson(raw, ApiError::class.java)
            val fieldError = parsed?.errors?.values?.firstOrNull()?.takeIf { it.isNotBlank() }
            val message = parsed?.message?.takeIf { it.isNotBlank() }
            when {
                // Prefer a specific field error over a generic "Validation failed".
                fieldError != null && (message == null || message.contains("valid", true)) -> fieldError
                message != null -> message
                else -> defaultMessage(response.code())
            }
        }
    } catch (e: JsonSyntaxException) {
        defaultMessage(response.code())
    } catch (e: Exception) {
        defaultMessage(response.code())
    }
}

private fun defaultMessage(code: Int): String = when (code) {
    401 -> "Invalid credentials or session expired"
    403 -> "You don't have permission to do that"
    404 -> "Not found"
    409 -> "Already exists"
    else -> "Request failed ($code)"
}
