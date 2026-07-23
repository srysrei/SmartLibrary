package com.smartlibrary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartlibrary.data.dao.BookDao
import com.smartlibrary.data.dao.CategoryDao
import com.smartlibrary.data.entity.Book
import com.smartlibrary.data.entity.Category

/**
 * Local offline cache. Books and categories fetched from the API are written here so the
 * catalog still renders when the network is down. Users/borrows are always fetched live.
 */
@Database(entities = [Book::class, Category::class], version = 5, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        fun getDatabase(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "library_database",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
