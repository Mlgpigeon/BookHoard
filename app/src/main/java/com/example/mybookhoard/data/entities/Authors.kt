package com.example.mybookhoard.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

/**
 * Author entity matching BookHoardAPI authors table
 */
@Entity(tableName = "authors")
data class Author(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "biography")
    val biography: String? = null,

    @ColumnInfo(name = "birth_date")
    val birthDate: Date? = null,

    @ColumnInfo(name = "death_date")
    val deathDate: Date? = null,

    @ColumnInfo(name = "nationality")
    val nationality: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "trivia")
    val trivia: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)