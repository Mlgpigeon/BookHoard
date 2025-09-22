package com.example.mybookhoard.api

import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus
import org.json.JSONObject

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