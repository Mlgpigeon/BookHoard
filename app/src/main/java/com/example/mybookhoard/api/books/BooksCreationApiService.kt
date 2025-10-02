package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * API service for creating books and managing authors/sagas
 * Path: app/src/main/java/com/example/mybookhoard/api/books/BooksCreationApiService.kt
 *
 * Note: Uses ApiBook from ApiBook.kt for responses
 */
class BooksCreationApiService(private val context: Context) {
    companion object {
        private const val TAG = "BooksCreationApi"
    }

    private val apiClient = BooksApiClient(context)

    /**
     * Search authors by name for suggestions
     */
    suspend fun searchAuthors(query: String): List<String> {
        if (query.isBlank() || query.length < 2) return emptyList()

        val response = apiClient.makeAuthenticatedRequest("authors/search?q=${query.trim()}", "GET")

        return when {
            response.isSuccessful() -> {
                try {
                    Log.d(TAG, "Search authors response: ${response.body}")
                    val json = JSONObject(response.body)

                    if (!json.has("success") || !json.getBoolean("success")) {
                        Log.w(TAG, "Authors search API returned success=false")
                        return emptyList()
                    }

                    val data = json.getJSONObject("data")
                    val authorsArray = data.getJSONArray("authors")

                    val authors = mutableListOf<String>()
                    for (i in 0 until authorsArray.length()) {
                        val authorObj = authorsArray.getJSONObject(i)
                        val name = authorObj.getString("name")
                        authors.add(name)
                    }

                    Log.d(TAG, "Found ${authors.size} authors matching '$query'")
                    authors.distinct().sorted()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse authors response", e)
                    Log.e(TAG, "Response was: ${response.body}")
                    emptyList()
                }
            }
            else -> {
                Log.e(TAG, "Authors search failed with code ${response.code}: ${response.body}")
                emptyList()
            }
        }
    }

    /**
     * Search sagas by name for suggestions
     */
    suspend fun searchSagas(query: String): List<SagaSuggestion> {
        if (query.isBlank() || query.length < 2) return emptyList()

        val response = apiClient.makeAuthenticatedRequest("sagas/search?q=${query.trim()}", "GET")

        return when {
            response.isSuccessful() -> {
                try {
                    Log.d(TAG, "Search sagas response: ${response.body}")
                    val json = JSONObject(response.body)

                    if (!json.has("success") || !json.getBoolean("success")) {
                        Log.w(TAG, "Sagas search API returned success=false")
                        return emptyList()
                    }

                    val data = json.getJSONObject("data")
                    val sagasArray = data.getJSONArray("sagas")

                    val sagas = mutableListOf<SagaSuggestion>()
                    for (i in 0 until sagasArray.length()) {
                        val sagaObj = sagasArray.getJSONObject(i)
                        sagas.add(
                            SagaSuggestion(
                                id = sagaObj.getLong("id"),
                                name = sagaObj.getString("name"),
                                totalBooks = sagaObj.optInt("total_books", 0),
                                isCompleted = sagaObj.optInt("is_completed", 0) == 1
                            )
                        )
                    }

                    Log.d(TAG, "Found ${sagas.size} sagas matching '$query'")
                    sagas
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse sagas response", e)
                    Log.e(TAG, "Response was: ${response.body}")
                    emptyList()
                }
            }
            else -> {
                Log.e(TAG, "Sagas search failed with code ${response.code}: ${response.body}")
                emptyList()
            }
        }
    }

    /**
     * Find or create an author, returns author ID
     */
    private suspend fun findOrCreateAuthor(authorName: String): AuthorResult {
        try {
            val trimmedName = authorName.trim()
            Log.d(TAG, "Finding or creating author: $trimmedName")

            // First, try to find existing author
            val searchResponse = apiClient.makeAuthenticatedRequest(
                "authors/search?q=${trimmedName}",
                "GET"
            )

            if (searchResponse.isSuccessful()) {
                val json = JSONObject(searchResponse.body)
                if (json.getBoolean("success")) {
                    val data = json.getJSONObject("data")
                    val authorsArray = data.getJSONArray("authors")

                    // Look for exact match (case-insensitive)
                    for (i in 0 until authorsArray.length()) {
                        val authorObj = authorsArray.getJSONObject(i)
                        val name = authorObj.getString("name")
                        if (name.equals(trimmedName, ignoreCase = true)) {
                            val authorId = authorObj.getLong("id")
                            Log.d(TAG, "Found existing author: $name (ID: $authorId)")
                            return AuthorResult.Success(authorId)
                        }
                    }
                }
            }

            // If not found, create new author
            Log.d(TAG, "Author not found, creating new: $trimmedName")
            val createData = JSONObject().apply {
                put("name", trimmedName)
            }

            val createResponse = apiClient.makeAuthenticatedRequest("authors", "POST", createData)

            return when {
                createResponse.isSuccessful() -> {
                    val json = JSONObject(createResponse.body)
                    if (json.getBoolean("success")) {
                        val authorId = json.getJSONObject("data").getLong("id")
                        Log.d(TAG, "Created new author: $trimmedName (ID: $authorId)")
                        AuthorResult.Success(authorId)
                    } else {
                        AuthorResult.Error("API returned success=false")
                    }
                }
                else -> {
                    AuthorResult.Error("Failed to create author: ${createResponse.body}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in findOrCreateAuthor", e)
            return AuthorResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Create a new book with optional saga information
     */
    suspend fun createBook(
        title: String,
        authorName: String? = null,
        description: String? = null,
        publicationYear: Int? = null,
        language: String = "en",
        isbn: String? = null,
        sagaId: Long? = null,
        sagaName: String? = null,
        sagaNumber: Int? = null
    ): BookCreationResult {
        try {
            Log.d(TAG, "Creating book: title='$title', author='$authorName', saga='$sagaName', sagaId=$sagaId, sagaNumber=$sagaNumber")

            // Handle author first if provided
            var authorId: Long? = null
            if (!authorName.isNullOrBlank()) {
                when (val authorResult = findOrCreateAuthor(authorName)) {
                    is AuthorResult.Success -> {
                        authorId = authorResult.id
                        Log.d(TAG, "Using author ID $authorId for '$authorName'")
                    }
                    is AuthorResult.Error -> {
                        Log.e(TAG, "Author creation/lookup failed: ${authorResult.message}")
                        return BookCreationResult.Error("Author error: ${authorResult.message}")
                    }
                }
            }

            // Create the book
            val bookData = JSONObject().apply {
                put("title", title.trim())
                authorId?.let { put("primary_author_id", it) }
                description?.let { if (it.isNotBlank()) put("description", it.trim()) }
                publicationYear?.let { put("publication_year", it) }
                put("language", language)
                isbn?.let { if (it.isNotBlank()) put("isbn", it.trim()) }

                // Saga information
                sagaId?.let { put("saga_id", it) }
                sagaName?.let { if (it.isNotBlank()) put("saga_name", it.trim()) }
                sagaNumber?.let { put("saga_number", it) }

                put("is_public", true)
                put("source", "user_defined")
            }

            Log.d(TAG, "Sending book creation request: $bookData")
            val response = apiClient.makeAuthenticatedRequest("books", "POST", bookData)

            return when {
                response.isSuccessful() -> {
                    try {
                        Log.d(TAG, "Create book response: ${response.body}")
                        val json = JSONObject(response.body)

                        if (!json.has("success") || !json.getBoolean("success")) {
                            return BookCreationResult.Error("API returned success=false")
                        }

                        val data = json.getJSONObject("data")
                        val book = ApiBook.fromJson(data)

                        Log.d(TAG, "Book created successfully: ${book.title}")
                        BookCreationResult.Success(book)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse create book response", e)
                        Log.e(TAG, "Response was: ${response.body}")
                        BookCreationResult.Error("Failed to parse response: ${e.message}")
                    }
                }
                else -> {
                    Log.e(TAG, "Book creation failed with code ${response.code}: ${response.body}")
                    BookCreationResult.Error("Failed to create book: ${response.body}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in createBook", e)
            return BookCreationResult.Error(e.message ?: "Unknown error")
        }
    }
}

/**
 * Data class for saga suggestions
 */
data class SagaSuggestion(
    val id: Long,
    val name: String,
    val totalBooks: Int,
    val isCompleted: Boolean
)

// Result classes for internal operations
sealed class AuthorResult {
    data class Success(val id: Long) : AuthorResult()
    data class Error(val message: String) : AuthorResult()
}

sealed class BookCreationResult {
    data class Success(val book: ApiBook) : BookCreationResult()
    data class Error(val message: String) : BookCreationResult()
}