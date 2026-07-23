package com.smartlibrary.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline cache of a book. Mirrors the API's BookResponse so the catalog can render
 * from Room when the network is unavailable.
 */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: Long,
    val title: String,
    val author: String,
    val categoryId: Long,
    val categoryName: String?,
    val description: String?,
    val qty: Int,
    val availableQty: Int,
    val imageUrl: String?,
)
