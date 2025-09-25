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
     * Search books in the remote API - includes user's collection status
     */
    suspend fun searchBooks(query: String, includeGoogleBooks: Boolean = true): BooksSearchResult {
        if (query.isBlank()) {
            return BooksSearchResult.Success(emptyList(), query)
        }

        val endpoint = if (includeGoogleBooks) {
            "books/search-google?q=${query.trim()}&include_google_books=true"
        } else {
            "books/search?q=${query.trim()}"
        }

        val response = makeAuthenticatedRequest(endpoint, "GET")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)
                    val data = json.getJSONObject("data")

                    val books = if (includeGoogleBooks && data.has("results")) {
                        // Enhanced search with Google Books
                        val results = data.getJSONObject("results")
                        val combinedBooks = results.optJSONArray("combined") ?: JSONArray()
                        parseBooksList(combinedBooks)
                    } else {
                        // Regular search
                        val booksArray = data.optJSONArray("books") ?: JSONArray()
                        parseBooksList(booksArray)
                    }

                    Log.d(TAG, "Search successful: found ${books.size} books for query '$query'")
                    BooksSearchResult.Success(books, query)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse search response", e)
                    BooksSearchResult.Error("Failed to parse search results: ${e.message}")
                }
            }
            else -> {
                val errorMessage = parseError(response.body)
                Log.e(TAG, "Search failed: $errorMessage")
                BooksSearchResult.Error(errorMessage)
            }
        }
    }

    /**
     * Get public books
     */
    suspend fun getPublicBooks(limit: Int = 50, offset: Int = 0): BooksSearchResult {
        val response = makeAuthenticatedRequest(
            "books/public?limit=$limit&offset=$offset",
            "GET"
        )

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)
                    val data = json.getJSONObject("data")
                    val booksArray = data.getJSONArray("books")
                    val books = parseBooksList(booksArray)

                    Log.d(TAG, "Got ${books.size} public books")
                    BooksSearchResult.Success(books, "")
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
                val book = ApiBook.fromJson(bookJson)
                books.add(book)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse book at index $i: ${e.message}")
            }
        }

        return books
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

    /**
     * Add book to user's collection via API
     */
    suspend fun addBookToCollection(
        bookData: Map<String, Any>,
        wishlistStatus: String
    ): BooksActionResult {
        val body = JSONObject().apply {
            bookData.forEach { (key, value) ->
                put(key, value)
            }
            put("wishlist", wishlistStatus)
            put("status", "NOT_STARTED")
        }

        val response = makeAuthenticatedRequest("books", "POST", body)

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
     * Remove book from user's collection via API
     */
    suspend fun removeBookFromCollection(bookId: Long): BooksActionResult {
        val response = makeAuthenticatedRequest("books/$bookId", "DELETE")

        return when {
            response.isSuccessful() -> {
                Log.d(TAG, "Book removed from collection successfully")
                BooksActionResult.Success("Book removed from collection")
            }
            else -> {
                val errorMessage = parseError(response.body)
                Log.e(TAG, "Remove book failed: $errorMessage")
                BooksActionResult.Error(errorMessage)
            }
        }
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