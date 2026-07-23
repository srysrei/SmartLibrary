package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.*
import com.smartlibrary.network.safeApiCall

class AuthRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun login(usernameOrEmail: String, password: String): ApiResult<AuthResponse> =
        safeApiCall { api.login(LoginRequest(usernameOrEmail, password)) }

    suspend fun register(
        username: String,
        fullName: String,
        email: String,
        phone: String,
        password: String,
    ): ApiResult<AuthResponse> =
        safeApiCall { api.register(RegisterRequest(username, fullName, email, phone, password)) }

    suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResponse> =
        safeApiCall { api.forgotPassword(ForgotPasswordRequest(email)) }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): ApiResult<MessageResponse> =
        safeApiCall { api.resetPassword(ResetPasswordRequest(email, otp, newPassword)) }

    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<MessageResponse> =
        safeApiCall { api.changePassword(ChangePasswordRequest(oldPassword, newPassword)) }
}
