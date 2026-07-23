package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.DashboardResponse
import com.smartlibrary.network.safeApiCall

class DashboardRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun getDashboard(): ApiResult<DashboardResponse> =
        safeApiCall { api.getDashboard() }
}
