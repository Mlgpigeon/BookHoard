package com.example.mybookhoard.api.books

import org.json.JSONObject
import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.BookSource
import java.util.Date

/**
 * Book model for API responses
 */
data class ApiBook(
    val id: Long?,
    val title: String,
    val originalTitle: String?,
    val description: String?,
    val author: String?,
    val authorId: Long?,
    val saga: String?,
    val sagaNumber: Int?,
    val language: String,
    val publicationYear: Int?,
    val genres: List<String>?,
    val isbn: String?,
    val coverSelected: String?,
    val images: List<String>?,
    val adaptations: List<String>?,
    val averageRating: Float,
    val totalRatings: Int,
    val isPublic: Boolean,
    val source: String,
    val sourceLabel: String?,
    val createdAt: String?,
    val updatedAt: String?,
    // Additional fields for Google Books
    val googleBooksId: String?,
    val publisher: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val categories: List<String>?,
    val imageLinks: Map<String, String>?,
    val canBeAdded: Boolean?
) {
    companion object {
        fun fromJson(json: JSONObject): ApiBook {
            return ApiBook(
                id = json.optLong("id", 0).takeIf { it != 0L },
                title = json.getString("title"),
                originalTitle = json.optString("original_title").takeIf { it.isNotBlank() },
                description = json.optString("description").takeIf { it.isNotBlank() },
                author = json.optString("author").takeIf { it.isNotBlank() },
                authorId = json.optLong("author_id", 0).takeIf { it != 0L },
                saga = json.optString("saga").takeIf { it.isNotBlank() },
                sagaNumber = json.optInt("saga_number", 0).takeIf { it != 0 },
                language = json.optString("language", "en"),
                publicationYear = json.optInt("publication_year", 0).takeIf { it != 0 },
                genres = json.optJSONArray("genres")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                },
                isbn = json.optString("isbn").takeIf { it.isNotBlank() },
                coverSelected = json.optString("cover_selected").takeIf { it.isNotBlank() },
                images = json.optJSONArray("images")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                },
                adaptations = json.optJSONArray("adaptations")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                },
                averageRating = json.optDouble("average_rating", 0.0).toFloat(),
                totalRatings = json.optInt("total_ratings", 0),
                isPublic = json.optBoolean("is_public", true),
                source = json.optString("source", "user_defined"),
                sourceLabel = json.optString("source_label"),
                createdAt = json.optString("created_at").takeIf { it.isNotBlank() },
                updatedAt = json.optString("updated_at").takeIf { it.isNotBlank() },
                // Google Books specific fields
                googleBooksId = json.optString("google_books_id").takeIf { it.isNotBlank() },
                publisher = json.optString("publisher").takeIf { it.isNotBlank() },
                publishedDate = json.optString("published_date").takeIf { it.isNotBlank() },
                pageCount = json.optInt("page_count", 0).takeIf { it != 0 },
                categories = json.optJSONArray("categories")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                },
                imageLinks = json.optJSONObject("image_links")?.let { obj ->
                    val map = mutableMapOf<String, String>()
                    obj.keys().forEach { key ->
                        map[key] = obj.getString(key)
                    }
                    map
                },
                canBeAdded = json.optBoolean("can_be_added")
            )
        }
    }

    /**
     * Convert ApiBook to Room Book entity for local storage
     */
    fun toBookEntity(): Book {
        return Book(
            id = id ?: 0,
            title = title,
            originalTitle = originalTitle,
            description = description,
            primaryAuthorId = authorId, // Use the authorId from API
            sagaId = null, // Will be resolved when needed
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
            source = when (source) {
                "google_books_api" -> BookSource.GOOGLE_BOOKS_API
                "openlibrary_api" -> BookSource.OPENLIBRARY_API
                else -> BookSource.USER_DEFINED
            },
            createdAt = Date(),
            updatedAt = Date()
        )
    }

    /**
     * Create a copy with updated canBeAdded status
     */
    fun copy(canBeAdded: Boolean): ApiBook {
        return this.copy(
            id = this.id,
            title = this.title,
            originalTitle = this.originalTitle,
            description = this.description,
            author = this.author,
            authorId = this.authorId,
            saga = this.saga,
            sagaNumber = this.sagaNumber,
            language = this.language,
            publicationYear = this.publicationYear,
            genres = this.genres,
            isbn = this.isbn,
            coverSelected = this.coverSelected,
            images = this.images,
            adaptations = this.adaptations,
            averageRating = this.averageRating,
            totalRatings = this.totalRatings,
            isPublic = this.isPublic,
            source = this.source,
            sourceLabel = this.sourceLabel,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            googleBooksId = this.googleBooksId,
            publisher = this.publisher,
            publishedDate = this.publishedDate,
            pageCount = this.pageCount,
            categories = this.categories,
            imageLinks = this.imageLinks,
            canBeAdded = canBeAdded
        )
    }
}