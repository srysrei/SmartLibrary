package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.data.LibraryDatabase
import com.smartlibrary.data.entity.Category
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.CategoryRequest
import com.smartlibrary.network.dto.CategoryResponse
import com.smartlibrary.network.dto.MessageResponse
import com.smartlibrary.network.safeApiCall
import kotlinx.coroutines.flow.first

class CategoryRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)
    private val categoryDao = LibraryDatabase.getDatabase(context).categoryDao()

    suspend fun fetchCategories(): ApiResult<List<CategoryResponse>> {
        val result = safeApiCall { api.getCategories() }
        if (result is ApiResult.Success) {
            categoryDao.deleteAllCategories()
            categoryDao.insertCategories(result.data.map {
                Category(id = it.id, name = it.name, bookCount = it.bookCount.toInt())
            })
        }
        return result
    }

    suspend fun cachedCategories(): List<Category> =
        categoryDao.getAllCategories().first()

    suspend fun createCategory(name: String): ApiResult<CategoryResponse> =
        safeApiCall { api.createCategory(CategoryRequest(name)) }

    suspend fun updateCategory(id: Long, name: String): ApiResult<CategoryResponse> =
        safeApiCall { api.updateCategory(id, CategoryRequest(name)) }

    suspend fun deleteCategory(id: Long): ApiResult<MessageResponse> =
        safeApiCall { api.deleteCategory(id) }
}
