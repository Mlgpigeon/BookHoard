package com.example.mybookhoard.api

import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.UserBook
import com.example.mybookhoard.data.UserBookReadingStatus
import com.example.mybookhoard.data.UserBookWishlistStatus
import com.example.mybookhoard.data.WishlistStatus
import org.json.JSONObject
import java.util.Date

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: String,
    val isActive: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): User {
            return User(
                id = json.getLong("id"),
                username = json.getString("username"),
                email = json.getString("email"),
                role = json.getString("role"),
                createdAt = json.getString("created_at"),
                isActive = json.getBoolean("is_active")
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("username", username)
            put("email", email)
            put("role", role)
            put("created_at", createdAt)
            put("is_active", isActive)
        }
    }
}

data class ApiBook(
    val id: Long? = null,
    val userId: Long? = null,
    val title: String,
    val author: String? = null,
    val saga: String? = null,
    val description: String? = null,
    val status: String = "NOT_STARTED", // "NOT_STARTED", "READING", "READ"
    val wishlist: String? = null, // null, "WISH", "ON_THE_WAY", "OBTAINED"
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): ApiBook {
            return ApiBook(
                id = if (json.has("id")) json.getLong("id") else null,
                userId = if (json.has("user_id")) json.getLong("user_id") else null,
                title = json.getString("title"),
                author = if (json.has("author") && !json.isNull("author"))
                    json.getString("author") else null,
                saga = if (json.has("saga") && !json.isNull("saga"))
                    json.getString("saga") else null,
                description = if (json.has("description") && !json.isNull("description"))
                    json.getString("description") else null,
                status = json.optString("status", "NOT_STARTED"),
                wishlist = if (json.has("wishlist") && !json.isNull("wishlist"))
                    json.getString("wishlist") else null,
                createdAt = if (json.has("created_at")) json.getString("created_at") else null,
                updatedAt = if (json.has("updated_at")) json.getString("updated_at") else null
            )
        }

        fun fromLocalBook(book: Book): ApiBook {
            return ApiBook(
                id = if (book.id == 0L) null else book.id,
                title = book.title,
                author = book.author,
                saga = book.saga,
                description = book.description,
                status = book.status.name,
                wishlist = book.wishlist?.name
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            if (id != null) put("id", id)
            if (userId != null) put("user_id", userId)
            put("title", title)
            if (author != null) put("author", author)
            if (saga != null) put("saga", saga)
            if (description != null) put("description", description)
            put("status", status)
            if (wishlist != null) put("wishlist", wishlist)
            if (createdAt != null) put("created_at", createdAt)
            if (updatedAt != null) put("updated_at", updatedAt)
        }
    }

    fun toLocalBook(): Book {
        val readingStatus = try {
            ReadingStatus.valueOf(status)
        } catch (e: Exception) {
            ReadingStatus.NOT_STARTED
        }

        val wishlistStatus = try {
            if (wishlist != null) WishlistStatus.valueOf(wishlist) else null
        } catch (e: Exception) {
            null
        }

        return Book(
            id = id ?: 0L,
            title = title,
            author = author,
            saga = saga,
            description = description,
            status = readingStatus,
            wishlist = wishlistStatus
        )
    }
}

// Connection state management
sealed class ConnectionState {
    object Online : ConnectionState()
    object Offline : ConnectionState()
    object Syncing : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

// Sync operation results
sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
    data class Partial(val message: String, val successful: Int, val failed: Int) : SyncResult()
}

// Authentication state
sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: User, val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Add to app/src/main/java/com/example/mybookhoard/api/ApiModels.kt

// Search result that can contain both local and Google Books results
data class SearchResult(
    val id: Long? = null,
    val title: String,
    val author: String? = null,
    val saga: String? = null,
    val description: String? = null,
    val source: String = "local", // "local" or "google_books_api"
    val sourceLabel: String = "Local",
    val googleBooksId: String? = null,
    val status: String? = null, // Only for local books
    val wishlist: String? = null, // Only for local books
    val isInLibrary: Boolean = false // True if this Google Books result is already in user's library
) {
    companion object {
        fun fromLocalBook(book: Book): SearchResult {
            return SearchResult(
                id = book.id,
                title = book.title,
                author = book.author,
                saga = book.saga,
                description = book.description,
                source = "local",
                sourceLabel = "Your Library",
                status = book.status.name,
                wishlist = book.wishlist?.name,
                isInLibrary = true
            )
        }

        fun fromGoogleBook(json: JSONObject): SearchResult {
            return SearchResult(
                title = json.getString("title"),
                author = json.optString("author", null),
                saga = json.optString("saga", null),
                description = json.optString("description", null),
                source = json.optString("source", "google_books_api"),
                sourceLabel = json.optString("source_label", "Google Books"),
                googleBooksId = json.optString("google_books_id", null),
                isInLibrary = false
            )
        }
    }

    fun toApiBook(): ApiBook {
        return ApiBook(
            id = id,
            title = title,
            author = author,
            saga = saga,
            description = description,
            status = status ?: "NOT_STARTED"
        )
    }
}
data class ApiUserBook(
    val id: Long,
    val userId: Long,
    val bookId: Long,
    val title: String,
    val author: String? = null,
    val saga: String? = null,
    val description: String? = null,
    val readingStatus: String = "NOT_STARTED",
    val wishlistStatus: String? = null,
    val personalRating: Int? = null,
    val review: String? = null,
    val readingProgress: Int = 0,
    val favorite: Boolean = false,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun fromJson(json: JSONObject): ApiUserBook {
            return ApiUserBook(
                id = json.getLong("id"),
                userId = json.getLong("user_id"),
                bookId = json.getLong("book_id"),
                title = json.getString("title"),
                author = json.optString("author").takeIf { it.isNotEmpty() },
                saga = json.optString("saga").takeIf { it.isNotEmpty() },
                description = json.optString("description").takeIf { it.isNotEmpty() },
                readingStatus = json.getString("reading_status"),
                wishlistStatus = json.optString("wishlist_status").takeIf { it.isNotEmpty() },
                personalRating = if (json.isNull("personal_rating")) null else json.optInt("personal_rating"),
                review = json.optString("review").takeIf { it.isNotEmpty() },
                readingProgress = json.optInt("reading_progress", 0),
                favorite = json.optBoolean("favorite", false),
                createdAt = json.getString("created_at"),
                updatedAt = json.getString("updated_at")
            )
        }
    }

    fun toLocalUserBook(): UserBook {
        return UserBook(
            id = id,
            userId = userId,
            bookId = bookId,
            readingStatus = UserBookReadingStatus.valueOf(readingStatus),
            wishlistStatus = wishlistStatus?.let { UserBookWishlistStatus.valueOf(it) },
            personalRating = personalRating,
            review = review,
            readingProgress = readingProgress,
            favorite = favorite,
            createdAt = parseDate(createdAt),
            updatedAt = parseDate(updatedAt)
        )
    }

    private fun parseDate(dateString: String): Date {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}

// Add this extension to the existing SearchResult
fun SearchResult.toUserBookCreationData(userId: Long, wishlistStatus: UserBookWishlistStatus): Map<String, Any?> {
    return mapOf(
        "title" to title,
        "author" to author,
        "saga" to saga,
        "description" to description,
        "source" to source,
        "wishlist" to wishlistStatus.name,
        "status" to "NOT_STARTED"
    )
}

// Combined search response
data class CombinedSearchResponse(
    val localResults: List<SearchResult>,
    val googleResults: List<SearchResult>,
    val totalLocal: Int,
    val totalGoogle: Int,
    val query: String
) {
    val allResults: List<SearchResult>
        get() = localResults + googleResults

    val totalResults: Int
        get() = localResults.size + googleResults.size
}