package com.example.mybookhoard.api.books

import com.example.mybookhoard.data.entities.UserBook
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus

/**
 * API Models and Result classes for Books API
 * Path: app/src/main/java/com/example/mybookhoard/api/books/BooksApiModels.kt
 */

/**
 * API Response wrapper
 */
data class ApiResponse(val code: Int, val body: String) {
    fun isSuccessful(): Boolean = code in 200..299
}

/**
 * Result of books search operation
 */
sealed class BooksSearchResult {
    data class Success(val books: List<ApiBook>, val query: String) : BooksSearchResult()
    data class Error(val message: String) : BooksSearchResult()
}

/**
 * Result of books actions (add/remove)
 */
sealed class BooksActionResult {
    data class Success(val message: String) : BooksActionResult()
    data class Error(val message: String) : BooksActionResult()
}

/**
 * Result of user book operations
 */
sealed class UserBookResult {
    data class Success(val userBook: UserBook) : UserBookResult()
    data class Error(val message: String) : UserBookResult()
}

/**
 * Helper class to parse UserBook from JSON
 */
object UserBookParser {
    fun parseUserBookFromJson(json: org.json.JSONObject, bookId: Long, userId: Long): UserBook {
        return UserBook(
            id = json.getLong("id"),
            userId = userId,
            bookId = bookId,
            readingStatus = when (json.optString("reading_status", "not_started")) {
                "reading" -> UserBookReadingStatus.READING
                "read" -> UserBookReadingStatus.READ
                "abandoned" -> UserBookReadingStatus.ABANDONED
                else -> UserBookReadingStatus.NOT_STARTED
            },
            wishlistStatus = when (json.optString("wishlist_status")) {
                "wish" -> UserBookWishlistStatus.WISH
                "on_the_way" -> UserBookWishlistStatus.ON_THE_WAY
                "obtained" -> UserBookWishlistStatus.OBTAINED
                else -> null
            },
            personalRating = json.optDouble("personal_rating", 0.0).takeIf { it != 0.0 }?.toFloat(),
            review = json.optString("review").takeIf { it.isNotBlank() },
            annotations = json.optString("annotations").takeIf { it.isNotBlank() },
            readingProgress = json.optInt("reading_progress", 0),
            dateStarted = null, // Would need date parsing if needed
            dateFinished = null, // Would need date parsing if needed
            favorite = json.optInt("favorite", 0) == 1,
            createdAt = java.util.Date(), // Default to current date
            updatedAt = java.util.Date()  // Default to current date
        )
    }
}