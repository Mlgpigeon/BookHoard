package com.example.mybookhoard.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["title","author"], unique = true)]
)
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String? = null,
    val saga: String? = null,
    val description: String? = null,  // New field for book description
    val status: ReadingStatus = ReadingStatus.NOT_STARTED,
    val wishlist: WishlistStatus? = null   // null = no está en wishlist
)

enum class ReadingStatus {
    NOT_STARTED, // sin comenzar
    READING,     // leyendo
    READ         // leído
}

enum class WishlistStatus {
    WISH,       // quiero este libro
    ON_THE_WAY, // ya está en camino
    OBTAINED    // ya lo tengo
}