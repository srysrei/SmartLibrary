package com.smartlibrary.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartlibrary.data.entity.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY title")
    fun searchBooks(query: String): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE categoryId = :categoryId ORDER BY title")
    fun getBooksByCategory(categoryId: Long): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%') AND categoryId = :categoryId ORDER BY title")
    fun searchBooksByCategory(query: String, categoryId: Long): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<Book?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<Book>)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}
