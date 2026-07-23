package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.BorrowRequest
import com.smartlibrary.network.dto.BorrowResponse
import com.smartlibrary.network.dto.ReturnRequest
import com.smartlibrary.network.safeApiCall

class BorrowRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun createBorrow(
        bookIds: List<Long>,
        borrowDate: String,
        borrowDay: Int,
        description: String?,
    ): ApiResult<BorrowResponse> =
        safeApiCall { api.createBorrow(BorrowRequest(bookIds, borrowDate, borrowDay, description)) }

    suspend fun myBorrows(status: String? = null): ApiResult<List<BorrowResponse>> =
        safeApiCall { api.getMyBorrows(status) }

    suspend fun getBorrow(id: Long): ApiResult<BorrowResponse> =
        safeApiCall { api.getBorrow(id) }

    suspend fun returnBooks(id: Long, borrowBookIds: List<Long>): ApiResult<BorrowResponse> =
        safeApiCall { api.returnBooks(id, ReturnRequest(borrowBookIds)) }

    // Admin
    suspend fun allBorrows(status: String? = null): ApiResult<List<BorrowResponse>> =
        safeApiCall { api.getAllBorrows(status) }

    suspend fun approve(id: Long): ApiResult<BorrowResponse> =
        safeApiCall { api.approveBorrow(id) }

    suspend fun reject(id: Long): ApiResult<BorrowResponse> =
        safeApiCall { api.rejectBorrow(id) }

    suspend fun reverse(id: Long): ApiResult<BorrowResponse> =
        safeApiCall { api.reverseBorrow(id) }
}
