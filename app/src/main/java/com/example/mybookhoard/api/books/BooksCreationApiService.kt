package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * API service for creating books and managing authors
 * Path: app/src/main/java/com/example/mybookhoard/api/books/BooksCreationApiService.kt
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
                    Log.e(TAG, "Response body was: ${response.body}")
                    emptyList()
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.w(TAG, "Search authors failed: ${response.code} - $errorMessage")
                emptyList()
            }
        }
    }

    /**
     * Find or create author by name
     */
    suspend fun findOrCreateAuthor(authorName: String): AuthorResult {
        if (authorName.isBlank()) {
            return AuthorResult.Error("Author name cannot be blank")
        }

        // First, try to find existing author by getting all authors
        val getAllResponse = apiClient.makeAuthenticatedRequest("authors", "GET")
        if (getAllResponse.isSuccessful()) {
            try {
                Log.d(TAG, "Get all authors response: ${getAllResponse.body}")
                val json = JSONObject(getAllResponse.body)

                if (json.has("success") && json.getBoolean("success")) {
                    val data = json.getJSONObject("data")
                    val authorsArray = data.getJSONArray("authors")

                    for (i in 0 until authorsArray.length()) {
                        val authorObj = authorsArray.getJSONObject(i)
                        val name = authorObj.getString("name")
                        if (name.equals(authorName, ignoreCase = true)) {
                            Log.d(TAG, "Found existing author: $name with ID ${authorObj.getLong("id")}")
                            return AuthorResult.Success(
                                id = authorObj.getLong("id"),
                                name = name
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse existing authors, will try to create new one", e)
            }
        }

        // If not found, create new author
        val createBody = JSONObject().apply {
            put("name", authorName.trim())
        }

        Log.d(TAG, "Creating new author: $authorName")
        val createResponse = apiClient.makeAuthenticatedRequest("authors", "POST", createBody)

        return when {
            createResponse.isSuccessful() -> {
                try {
                    Log.d(TAG, "Create author response: ${createResponse.body}")
                    val json = JSONObject(createResponse.body)

                    if (!json.has("success") || !json.getBoolean("success")) {
                        return AuthorResult.Error("API returned success=false")
                    }

                    val data = json.getJSONObject("data")
                    val result = AuthorResult.Success(
                        id = data.getLong("id"),
                        name = data.getString("name")
                    )

                    Log.d(TAG, "Successfully created author: ${result}")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse create author response", e)
                    Log.e(TAG, "Response body was: ${createResponse.body}")
                    AuthorResult.Error("Failed to parse author creation response: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(createResponse.body)
                Log.e(TAG, "Create author failed: ${createResponse.code} - $errorMessage")
                Log.e(TAG, "Response body was: ${createResponse.body}")
                AuthorResult.Error("Failed to create author: $errorMessage")
            }
        }
    }

    /**
     * Create a new book
     */
    suspend fun createBook(
        title: String,
        authorName: String?,
        description: String?,
        publicationYear: Int?,
        language: String,
        isbn: String?
    ): BookCreationResult {
        try {
            Log.d(TAG, "Creating book: title='$title', author='$authorName', year=$publicationYear")

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
                put("is_public", true) // Make books public by default
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
                        Log.e(TAG, "Response body was: ${response.body}")
                        BookCreationResult.Error("Failed to parse book creation response: ${e.message}")
                    }
                }
                else -> {
                    val errorMessage = apiClient.parseError(response.body)
                    Log.e(TAG, "Create book failed: ${response.code} - $errorMessage")
                    Log.e(TAG, "Response body was: ${response.body}")
                    BookCreationResult.Error("Failed to create book: $errorMessage")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception creating book", e)
            return BookCreationResult.Error("Error creating book: ${e.message}")
        }
    }
}

/**
 * Result classes for API operations
 */
sealed class AuthorResult {
    data class Success(val id: Long, val name: String) : AuthorResult()
    data class Error(val message: String) : AuthorResult()
}

sealed class BookCreationResult {
    data class Success(val book: ApiBook) : BookCreationResult()
    data class Error(val message: String) : BookCreationResult()
}