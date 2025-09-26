package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for book-related API operations (search, public books, etc.)
 * Path: app/src/main/java/com/example/mybookhoard/api/books/BooksApiService.kt
 */
class BooksApiService(private val context: Context) {
    companion object {
        private const val TAG = "BooksApiService"
    }

    private val apiClient = BooksApiClient(context)

    /**
     * Search books in the remote API - uses dedicated search endpoint
     */
    suspend fun searchBooks(query: String, includeGoogleBooks: Boolean = true): BooksSearchResult {
        if (query.isBlank()) {
            return BooksSearchResult.Success(emptyList(), query)
        }

        val response = apiClient.makeAuthenticatedRequest("books/search?q=${query.trim()}", "GET")

        return when {
            response.isSuccessful() -> {
                try {
                    Log.d(TAG, "Raw search response: ${response.body}")
                    val json = JSONObject(response.body)

                    if (!json.has("success") || !json.getBoolean("success")) {
                        return BooksSearchResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    val booksArray = data.getJSONArray("books") ?: JSONArray()

                    Log.d(TAG, "Found ${booksArray.length()} books in API response")

                    val books = parseBooksList(booksArray)
                    val booksWithUserStatus = checkUserCollectionStatus(books)

                    Log.d(TAG, "Search successful: found ${booksWithUserStatus.size} books for query '$query'")
                    BooksSearchResult.Success(booksWithUserStatus, query)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse search response", e)
                    Log.e(TAG, "Response body was: ${response.body}")
                    BooksSearchResult.Error("Failed to parse search results: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Search failed with code ${response.code}: $errorMessage")
                BooksSearchResult.Error(errorMessage)
            }
        }
    }

    /**
     * Get public books
     */
    suspend fun getPublicBooks(limit: Int = 50, offset: Int = 0): BooksSearchResult {
        val response = apiClient.makeAuthenticatedRequest("books/public", "GET")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)
                    val data = json.getJSONObject("data")
                    val booksArray = data.getJSONArray("books")
                    val books = parseBooksList(booksArray)
                    val booksWithUserStatus = checkUserCollectionStatus(books)

                    Log.d(TAG, "Got ${booksWithUserStatus.size} public books")
                    BooksSearchResult.Success(booksWithUserStatus, "")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse public books response", e)
                    BooksSearchResult.Error("Failed to parse public books: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Get public books failed: $errorMessage")
                BooksSearchResult.Error(errorMessage)
            }
        }
    }

    /**
     * Get book by ID
     */
    suspend fun getBookById(bookId: Long): ApiBook? {
        val response = apiClient.makeAuthenticatedRequest("books/$bookId", "GET")
        return if (response.isSuccessful()) {
            try {
                val root = JSONObject(response.body)
                // Support both wrapped and direct object formats
                val obj: JSONObject = when {
                    root.has("data") -> root.getJSONObject("data")
                    else -> root
                }
                ApiBook.fromJson(obj)
            } catch (e: Exception) {
                Log.e(TAG, "getBookById - parse error", e)
                null
            }
        } else {
            Log.w(TAG, "getBookById - request failed ${response.code}")
            null
        }
    }

    private fun parseBooksList(booksArray: JSONArray): List<ApiBook> {
        val books = mutableListOf<ApiBook>()

        for (i in 0 until booksArray.length()) {
            try {
                val bookJson = booksArray.getJSONObject(i)
                Log.d(TAG, "Parsing book $i: $bookJson")
                val book = ApiBook.fromJson(bookJson)
                books.add(book)
                Log.d(TAG, "Successfully parsed book: ${book.title}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse book at index $i: ${e.message}")
            }
        }

        return books
    }

    private suspend fun checkUserCollectionStatus(books: List<ApiBook>): List<ApiBook> {
        val currentUserId = apiClient.getCurrentUserId()

        return books.map { book ->
            book.id?.let { bookId ->
                val hasInCollection = checkIfBookInUserCollection(bookId)
                book.copy(canBeAdded = !hasInCollection)
            } ?: book.copy(canBeAdded = true)
        }
    }

    private suspend fun checkIfBookInUserCollection(bookId: Long): Boolean {
        return try {
            val response = apiClient.makeAuthenticatedRequest("user_books/by-book/$bookId", "GET")
            if (response.isSuccessful()) {
                Log.d(TAG, "checkIfBookInUserCollection - Response: ${response.body}")
                val json = JSONObject(response.body)

                if (!json.getBoolean("success")) {
                    Log.w(TAG, "checkIfBookInUserCollection - API returned success=false")
                    return false
                }

                val data = json.getJSONObject("data")
                val userBooks = if (data.has("userbooks")) {
                    data.getJSONArray("userbooks")
                } else {
                    Log.w(TAG, "checkIfBookInUserCollection - Using fallback for old response format")
                    if (data is JSONArray) {
                        data as JSONArray
                    } else {
                        JSONArray()
                    }
                }

                val currentUserId = apiClient.getCurrentUserId()
                for (i in 0 until userBooks.length()) {
                    val userBook = userBooks.getJSONObject(i)
                    val userBookUserId = userBook.getLong("user_id")
                    if (userBookUserId == currentUserId) {
                        Log.d(TAG, "checkIfBookInUserCollection - Found book in user collection")
                        return true
                    }
                }

                Log.d(TAG, "checkIfBookInUserCollection - Book not in user collection")
                false
            } else {
                Log.w(TAG, "checkIfBookInUserCollection - Request failed with code ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkIfBookInUserCollection - Exception for book $bookId", e)
            false
        }
    }
}