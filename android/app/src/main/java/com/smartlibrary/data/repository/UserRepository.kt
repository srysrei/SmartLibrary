package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.UpdateRoleRequest
import com.smartlibrary.network.dto.UserResponse
import com.smartlibrary.network.safeApiCall

/**
 * Admin-facing user management (list users, change roles). The signed-in user's
 * own profile lives in [ProfileRepository].
 */
class UserRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun listUsers(): ApiResult<List<UserResponse>> =
        safeApiCall { api.listUsers() }

    suspend fun updateRole(id: Long, role: String): ApiResult<UserResponse> =
        safeApiCall { api.updateUserRole(id, UpdateRoleRequest(role)) }
}
