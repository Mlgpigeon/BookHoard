package com.example.mybookhoard.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import java.util.Date

@Entity(
    tableName = "user_books",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["user_id"]),
        Index(value = ["user_id", "book_id"], unique = true),
        Index(value = ["user_id", "reading_status"]),
        Index(value = ["user_id", "wishlist_status"])
    ]
)
data class UserBook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "book_id")
    val bookId: Long,

    @ColumnInfo(name = "reading_status")
    val readingStatus: UserBookReadingStatus = UserBookReadingStatus.NOT_STARTED,

    @ColumnInfo(name = "wishlist_status")
    val wishlistStatus: UserBookWishlistStatus? = null,

    @ColumnInfo(name = "personal_rating")
    val personalRating: Int? = null, // 1-5 stars

    @ColumnInfo(name = "review")
    val review: String? = null,

    @ColumnInfo(name = "annotations")
    val annotations: String? = null,

    @ColumnInfo(name = "reading_progress")
    val readingProgress: Int = 0, // percentage 0-100

    @ColumnInfo(name = "date_started")
    val dateStarted: Date? = null,

    @ColumnInfo(name = "date_finished")
    val dateFinished: Date? = null,

    @ColumnInfo(name = "favorite")
    val favorite: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)

enum class UserBookReadingStatus {
    NOT_STARTED,
    READING,
    READ,
    ABANDONED
}

enum class UserBookWishlistStatus {
    WISH,
    ON_THE_WAY,
    OBTAINED
}