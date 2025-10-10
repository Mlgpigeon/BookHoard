package com.example.mybookhoard.data.entities

import androidx.room.ColumnInfo
import com.example.mybookhoard.data.entities.BookSource
import java.util.Date

/**
 * Result class for queries that combine Book and UserBook data
 * This class has flat fields that Room can easily map from cursor
 */
data class BookUserResult(
    // Book fields
    @ColumnInfo(name = "id") val bookId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "original_title") val originalTitle: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "primary_author_id") val primaryAuthorId: Long?,
    @ColumnInfo(name = "saga_id") val sagaId: Long?,
    @ColumnInfo(name = "saga_number") val sagaNumber: Int?,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "publication_year") val publicationYear: Int?,
    @ColumnInfo(name = "genres") val genres: List<String>?,
    @ColumnInfo(name = "isbn") val isbn: String?,
    @ColumnInfo(name = "cover_selected") val coverSelected: String?,
    @ColumnInfo(name = "images") val images: List<String>?,
    @ColumnInfo(name = "adaptations") val adaptations: List<String>?,
    @ColumnInfo(name = "average_rating") val averageRating: Float,
    @ColumnInfo(name = "total_ratings") val totalRatings: Int,
    @ColumnInfo(name = "is_public") val isPublic: Boolean,
    @ColumnInfo(name = "source") val source: BookSource,
    @ColumnInfo(name = "created_at") val bookCreatedAt: Date,
    @ColumnInfo(name = "updated_at") val bookUpdatedAt: Date,

    // UserBook fields (nullable when book is not in user's collection)
    @ColumnInfo(name = "ub_id") val userBookId: Long?,
    @ColumnInfo(name = "ub_user_id") val userId: Long?,
    @ColumnInfo(name = "ub_reading_status") val readingStatus: UserBookReadingStatus?,
    @ColumnInfo(name = "ub_wishlist_status") val wishlistStatus: UserBookWishlistStatus?,
    @ColumnInfo(name = "ub_personal_rating") val personalRating: Float?,
    @ColumnInfo(name = "ub_review") val review: String?,
    @ColumnInfo(name = "ub_annotations") val annotations: String?,
    @ColumnInfo(name = "ub_reading_progress") val readingProgress: Int?,
    @ColumnInfo(name = "ub_date_started") val dateStarted: Date?,
    @ColumnInfo(name = "ub_date_finished") val dateFinished: Date?,
    @ColumnInfo(name = "ub_favorite") val favorite: Boolean?,
    @ColumnInfo(name = "ub_created_at") val userBookCreatedAt: Date?,
    @ColumnInfo(name = "ub_updated_at") val userBookUpdatedAt: Date?
) {
    /**
     * Converts this flat result to BookWithUserData
     */
    fun toBookWithUserData(): BookWithUserData {
        val book = Book(
            id = bookId,
            title = title,
            originalTitle = originalTitle,
            description = description,
            primaryAuthorId = primaryAuthorId,
            sagaId = sagaId,
            sagaNumber = sagaNumber,
            language = language,
            publicationYear = publicationYear,
            genres = genres,
            isbn = isbn,
            coverSelected = coverSelected,
            images = images,
            adaptations = adaptations,
            averageRating = averageRating,
            totalRatings = totalRatings,
            isPublic = isPublic,
            source = source,
            createdAt = bookCreatedAt,
            updatedAt = bookUpdatedAt
        )

        val userBook = if (userBookId != null && userId != null) {
            UserBook(
                id = userBookId,
                userId = userId,
                bookId = bookId,
                readingStatus = readingStatus ?: UserBookReadingStatus.NOT_STARTED,
                wishlistStatus = wishlistStatus,
                personalRating = personalRating,
                review = review,
                annotations = annotations,
                readingProgress = readingProgress ?: 0,
                dateStarted = dateStarted,
                dateFinished = dateFinished,
                favorite = favorite ?: false,
                createdAt = userBookCreatedAt ?: Date(),
                updatedAt = userBookUpdatedAt ?: Date()
            )
        } else null

        return BookWithUserData(book, userBook)
    }
}