package com.example.mybookhoard.data.entities

import org.json.JSONObject
import java.util.Date

/**
 * Saga/Series entity
 * Represents a book series or saga
 */
data class Saga(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val primaryAuthorId: Long? = null,
    val authorName: String? = null,
    val totalBooks: Int = 0,
    val actualBooksCount: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
) {
    companion object {
        fun fromJson(json: JSONObject): Saga {
            return Saga(
                id = json.optLong("id", 0),
                name = json.getString("name"),
                description = json.optString("description").takeIf { it.isNotBlank() },
                primaryAuthorId = json.optLong("primary_author_id", 0).takeIf { it != 0L },
                authorName = json.optString("author_name").takeIf { it.isNotBlank() },
                totalBooks = json.optInt("total_books", 0),
                actualBooksCount = json.optInt("actual_books_count", 0),
                isCompleted = json.optInt("is_completed", 0) == 1,
                createdAt = null, // Could parse if needed
                updatedAt = null  // Could parse if needed
            )
        }
    }
}

/**
 * Book with saga order information
 * Used when managing books within a saga
 */
data class BookInSaga(
    val book: Book,
    val sagaNumber: Int?
)