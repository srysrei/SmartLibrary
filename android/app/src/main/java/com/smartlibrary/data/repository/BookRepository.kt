package com.smartlibrary.data.repository

import android.content.Context
import com.smartlibrary.data.LibraryDatabase
import com.smartlibrary.data.entity.Book
import com.smartlibrary.network.ApiResult
import com.smartlibrary.network.RetrofitClient
import com.smartlibrary.network.dto.BookRequest
import com.smartlibrary.network.dto.BookResponse
import com.smartlibrary.network.dto.MessageResponse
import com.smartlibrary.network.safeApiCall
import kotlinx.coroutines.flow.first
import okhttp3.MultipartBody

/**
 * Books come from the API but are written through to Room so the catalog still renders
 * offline. When a network call fails, callers can fall back to [cachedBooks].
 */
class BookRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)
    private val bookDao = LibraryDatabase.getDatabase(context).bookDao()

    suspend fun fetchBooks(search: String?, categoryId: Long?): ApiResult<List<BookResponse>> {
        val result = safeApiCall { api.getBooks(search?.ifBlank { null }, categoryId) }
        if (result is ApiResult.Success) {
            // Only replace the full cache on an unfiltered fetch, otherwise just upsert.
            if (search.isNullOrBlank() && categoryId == null) {
                bookDao.deleteAllBooks()
            }
            bookDao.insertBooks(result.data.map { it.toEntity() })
        }
        return result
    }

    suspend fun cachedBooks(search: String?, categoryId: Long?): List<Book> {
        val flow = when {
            !search.isNullOrBlank() && categoryId != null ->
                bookDao.searchBooksByCategory(search, categoryId)
            !search.isNullOrBlank() -> bookDao.searchBooks(search)
            categoryId != null -> bookDao.getBooksByCategory(categoryId)
            else -> bookDao.getAllBooks()
        }
        return flow.first()
    }

    suspend fun getBook(id: Long): ApiResult<BookResponse> =
        safeApiCall { api.getBook(id) }

    /** Uploads a cover image for an existing book and returns the updated book. */
    suspend fun uploadImage(id: Long, part: MultipartBody.Part): ApiResult<BookResponse> =
        safeApiCall { api.uploadBookImage(id, part) }

    suspend fun createBook(body: BookRequest): ApiResult<BookResponse> =
        safeApiCall { api.createBook(body) }

    suspend fun updateBook(id: Long, body: BookRequest): ApiResult<BookResponse> =
        safeApiCall { api.updateBook(id, body) }

    suspend fun deleteBook(id: Long): ApiResult<MessageResponse> =
        safeApiCall { api.deleteBook(id) }
}

fun BookResponse.toEntity(): Book = Book(
    id = id,
    title = title,
    author = author,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    qty = qty,
    availableQty = availableQty,
    imageUrl = imageUrl,
)

/** Maps a cached row back to the DTO shape so offline reads feed the same UI. */
fun Book.toResponse(): BookResponse = BookResponse(
    id = id,
    title = title,
    author = author,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    qty = qty,
    availableQty = availableQty,
    imageUrl = imageUrl,
)
