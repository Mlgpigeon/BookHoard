package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Service for interacting with BookHoard API books endpoints
 */
class BooksApiService(private val context: Context) {
    companion object {
        private const val BASE_URL = "https://api.mybookhoard.com/api"
        private const val TAG = "BooksApiService"

        private const val CONNECT_TIMEOUT = 10_000
        private const val READ_TIMEOUT = 15_000
        private const val REQUEST_TIMEOUT = 20_000L
    }

    /**
     * Search books in the remote API - uses dedicated search endpoint
     */
    suspend fun searchBooks(query: String, includeGoogleBooks: Boolean = true): BooksSearchResult {
        if (query.isBlank()) {
            return BooksSearchResult.Success(emptyList(), query)
        }

        // Use the search endpoint with query parameter
        val response = makeAuthenticatedRequest("books/search?q=${query.trim()}", "GET")

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

                    // Parse books from search results
                    val books = parseBooksList(booksArray)

                    Log.d(TAG, "Successfully parsed ${books.size} books")

                    // For each book, check if user already has it in collection
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
                val errorMessage = parseError(response.body)
                Log.e(TAG, "Search failed with code ${response.code}: $errorMessage")
                Log.e(TAG, "Response body: ${response.body}")
                BooksSearchResult.Error(errorMessage)
            }
        }
    }

    /**
     * Check user collection status for each book
     */
    private suspend fun checkUserCollectionStatus(books: List<ApiBook>): List<ApiBook> {
        return books.map { book ->
            book.id?.let { bookId ->
                // Check if user has this book in collection
                val hasInCollection = checkIfBookInUserCollection(bookId)
                book.copy(canBeAdded = !hasInCollection)
            } ?: book.copy(canBeAdded = true)
        }
    }

    /**
     * Check if a book is already in user's collection
     */
    private suspend fun checkIfBookInUserCollection(bookId: Long): Boolean {
        return try {
            val response = makeAuthenticatedRequest("user_books/by-book/$bookId", "GET")
            if (response.isSuccessful()) {
                val json = JSONObject(response.body)
                val data = json.getJSONObject("data")
                val userBooks = data.getJSONArray("userbooks")

                // Check if current user has this book
                val currentUserId = getCurrentUserId()
                for (i in 0 until userBooks.length()) {
                    val userBook = userBooks.getJSONObject(i)
                    if (userBook.getLong("user_id") == currentUserId) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check user collection status for book $bookId", e)
            false
        }
    }

    /**
     * Get current user ID from shared preferences
     */
    private fun getCurrentUserId(): Long {
        val prefs = context.getSharedPreferences("bookhoard_auth", Context.MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }

    /**
     * Get public books
     */
    suspend fun getPublicBooks(limit: Int = 50, offset: Int = 0): BooksSearchResult {
        val response = makeAuthenticatedRequest("books/public", "GET")

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
                val errorMessage = parseError(response.body)
                Log.e(TAG, "Get public books failed: $errorMessage")
                BooksSearchResult.Error(errorMessage)
            }
        }
    }

    /**
     * Parse JSON array of books
     */
    private fun parseBooksList(booksArray: JSONArray): List<ApiBook> {
        val books = mutableListOf<ApiBook>()

        for (i in 0 until booksArray.length()) {
            try {
                val bookJson = booksArray.getJSONObject(i)
                Log.d(TAG, "Parsing book $i: ${bookJson}")
                val book = ApiBook.fromJson(bookJson)
                books.add(book)
                Log.d(TAG, "Successfully parsed book: ${book.title}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse book at index $i: ${e.message}")
                Log.w(TAG, "Book JSON was: ${booksArray.optJSONObject(i)}")
            }
        }

        return books
    }

    /**
     * Add book to user's collection via API - Creates user_book relationship
     */
    suspend fun addBookToCollection(
        bookId: Long,
        wishlistStatus: String
    ): BooksActionResult {
        val currentUserId = getCurrentUserId()
        if (currentUserId == -1L) {
            return BooksActionResult.Error("User not authenticated")
        }

        val body = JSONObject().apply {
            put("user_id", currentUserId)
            put("book_id", bookId)
            put("reading_status", "not_started")
            put("wishlist_status", when(wishlistStatus) {
                "WISH" -> "wish"
                "ON_THE_WAY" -> "on_the_way"
                "OBTAINED" -> "obtained"
                else -> "wish"
            })
        }

        val response = makeAuthenticatedRequest("user_books", "POST", body)

        return when {
            response.isSuccessful() -> {
                Log.d(TAG, "Book added to collection successfully")
                BooksActionResult.Success("Book added to collection")
            }
            else -> {
                val errorMessage = parseError(response.body)
                Log.e(TAG, "Add book failed: $errorMessage")
                BooksActionResult.Error(errorMessage)
            }
        }
    }

    /**
     * Remove book from user's collection via API - Deletes user_book relationship
     */
    suspend fun removeBookFromCollection(bookId: Long): BooksActionResult {
        val currentUserId = getCurrentUserId()
        if (currentUserId == -1L) {
            return BooksActionResult.Error("User not authenticated")
        }

        // First, find the user_book ID
        val getUserBookResponse = makeAuthenticatedRequest("user_books/by-book/$bookId", "GET")

        if (!getUserBookResponse.isSuccessful()) {
            return BooksActionResult.Error("Failed to find book in collection")
        }

        try {
            val json = JSONObject(getUserBookResponse.body)
            val data = json.getJSONObject("data")
            val userBooks = data.getJSONArray("userbooks")

            var userBookId: Long? = null
            for (i in 0 until userBooks.length()) {
                val userBook = userBooks.getJSONObject(i)
                if (userBook.getLong("user_id") == currentUserId) {
                    userBookId = userBook.getLong("id")
                    break
                }
            }

            if (userBookId == null) {
                return BooksActionResult.Error("Book not found in your collection")
            }

            // Delete the user_book relationship
            val deleteResponse = makeAuthenticatedRequest("user_books/$userBookId", "DELETE")

            return when {
                deleteResponse.isSuccessful() -> {
                    Log.d(TAG, "Book removed from collection successfully")
                    BooksActionResult.Success("Book removed from collection")
                }
                else -> {
                    val errorMessage = parseError(deleteResponse.body)
                    Log.e(TAG, "Remove book failed: $errorMessage")
                    BooksActionResult.Error(errorMessage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user books response", e)
            return BooksActionResult.Error("Failed to process removal")
        }
    }

    /**
     * Make authenticated request to the API
     */
    private suspend fun makeAuthenticatedRequest(
        endpoint: String,
        method: String,
        body: JSONObject? = null
    ): ApiResponse = withContext(Dispatchers.IO) {

        return@withContext withTimeoutOrNull(REQUEST_TIMEOUT) {
            try {
                val url = URL("$BASE_URL/$endpoint")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = method
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")

                    // Add auth token from shared preferences
                    val prefs = context.getSharedPreferences("bookhoard_auth", Context.MODE_PRIVATE)
                    val token = prefs.getString("auth_token", null)
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }

                body?.let {
                    connection.doOutput = true
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(it.toString())
                    }
                }

                val responseCode = connection.responseCode
                val responseBody = if (responseCode >= 400) {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use {
                        it.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.inputStream)).use {
                        it.readText()
                    }
                }

                Log.d(TAG, "$method $endpoint -> $responseCode")
                ApiResponse(responseCode, responseBody)

            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Request timeout for $endpoint: ${e.message}")
                ApiResponse(408, """{"message": "Request timeout. Please check your internet connection."}""")
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Network error for $endpoint: ${e.message}")
                ApiResponse(503, """{"message": "Unable to reach server. Please check your internet connection."}""")
            } catch (e: Exception) {
                Log.e(TAG, "Request failed for $endpoint: ${e.message}")
                ApiResponse(500, """{"message": "Network request failed: ${e.message}"}""")
            }
        } ?: ApiResponse(408, """{"message": "Request timeout"}""")
    }

    private fun parseError(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            json.optString("error", json.optString("message", "Unknown error occurred"))
        } catch (e: Exception) {
            "Unable to connect to server."
        }
    }
}

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