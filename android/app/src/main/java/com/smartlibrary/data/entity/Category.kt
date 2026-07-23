package com.smartlibrary.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Offline cache of a category. Mirrors the API's CategoryResponse. */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: Long,
    val name: String,
    val bookCount: Int = 0,
)
