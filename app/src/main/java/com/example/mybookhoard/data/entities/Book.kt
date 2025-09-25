package com.example.mybookhoard.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["title"]),
        Index(value = ["primary_author_id"]),
        Index(value = ["saga_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Author::class,
            parentColumns = ["id"],
            childColumns = ["primary_author_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "original_title")
    val originalTitle: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "primary_author_id")
    val primaryAuthorId: Long? = null,

    @ColumnInfo(name = "saga_id")
    val sagaId: Long? = null,

    @ColumnInfo(name = "saga_number")
    val sagaNumber: Int? = null,

    @ColumnInfo(name = "language")
    val language: String = "en",

    @ColumnInfo(name = "publication_year")
    val publicationYear: Int? = null,

    @ColumnInfo(name = "genres")
    val genres: List<String>? = null,

    @ColumnInfo(name = "isbn")
    val isbn: String? = null,

    @ColumnInfo(name = "cover_selected")
    val coverSelected: String? = null,

    @ColumnInfo(name = "images")
    val images: List<String>? = null,

    @ColumnInfo(name = "adaptations")
    val adaptations: List<String>? = null,

    @ColumnInfo(name = "average_rating")
    val averageRating: Float = 0.0f,

    @ColumnInfo(name = "total_ratings")
    val totalRatings: Int = 0,

    @ColumnInfo(name = "is_public")
    val isPublic: Boolean = true,

    @ColumnInfo(name = "source")
    val source: BookSource = BookSource.USER_DEFINED,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)

enum class BookSource {
    USER_DEFINED,
    GOOGLE_BOOKS_API,
    OPENLIBRARY_API
}