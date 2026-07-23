package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.UpdateProfileRequest
import com.smartlibrary.network.dto.UserResponse
import com.smartlibrary.network.safeApiCall
import okhttp3.MultipartBody

/**
 * Handles the *signed-in user's own* profile (view/update). Kept separate from
 * [UserRepository], which is the admin-facing user-management API.
 */
class ProfileRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun getProfile(): ApiResult<UserResponse> =
        safeApiCall { api.getProfile() }

    suspend fun updateProfile(
        username: String,
        fullName: String,
        email: String,
        phone: String,
    ): ApiResult<UserResponse> =
        safeApiCall { api.updateProfile(UpdateProfileRequest(username, fullName, email, phone)) }

    /** Uploads the signed-in user's profile image and returns the updated user. */
    suspend fun uploadImage(part: MultipartBody.Part): ApiResult<UserResponse> =
        safeApiCall { api.uploadUserImage(part) }
}
