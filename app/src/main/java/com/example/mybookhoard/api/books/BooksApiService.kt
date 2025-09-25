package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import com.example.mybookhoard.data.entities.UserBook
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
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
        val currentUserId = getCurrentUserId()

        return books.map { book ->
            book.id?.let { bookId ->
                // Check if user has this book in collection and get real data
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
                Log.d(TAG, "checkIfBookInUserCollection - Response: ${response.body}")
                val json = JSONObject(response.body)

                if (!json.getBoolean("success")) {
                    Log.w(TAG, "checkIfBookInUserCollection - API returned success=false")
                    return false
                }

                val data = json.getJSONObject("data")

                // 🔧 FIXED: Handle the corrected response format from PHP
                val userBooks = if (data.has("userbooks")) {
                    data.getJSONArray("userbooks")
                } else {
                    // Handle old format if PHP hasn't been updated yet
                    Log.w(TAG, "checkIfBookInUserCollection - Using fallback for old response format")
                    if (data is JSONArray) {
                        data as JSONArray
                    } else {
                        JSONArray()
                    }
                }

                // Check if current user has this book
                val currentUserId = getCurrentUserId()
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
            Log.e(TAG, "addBookToCollection - User not authenticated")
            return BooksActionResult.Error("User not authenticated")
        }

        val wishlistStatusValue = when(wishlistStatus) {
            "WISH" -> "wish"
            "ON_THE_WAY" -> "on_the_way"
            "OBTAINED" -> "obtained"
            else -> "wish"
        }

        val body = JSONObject().apply {
            put("user_id", currentUserId)
            put("book_id", bookId)
            put("reading_status", "not_started")
            put("wishlist_status", wishlistStatusValue)
        }

        Log.d(TAG, "addBookToCollection - Sending request: ${body}")

        val response = makeAuthenticatedRequest("user_books", "POST", body)

        return when {
            response.isSuccessful() -> {
                Log.d(TAG, "addBookToCollection - Response: ${response.body}")
                try {
                    // Parse the response to verify it was created correctly
                    val json = JSONObject(response.body)
                    if (json.getBoolean("success")) {
                        val data = json.getJSONObject("data")
                        val createdWishlistStatus = data.optString("wishlist_status", "null")
                        Log.d(TAG, "Book added successfully - wishlist_status: $createdWishlistStatus")

                        if (createdWishlistStatus == "null") {
                            Log.w(TAG, "Warning: wishlist_status was saved as null, but request succeeded")
                        }

                        BooksActionResult.Success("Book added to collection")
                    } else {
                        val errorMsg = json.optString("error", "Unknown error")
                        Log.e(TAG, "API returned success=false: $errorMsg")
                        BooksActionResult.Error(errorMsg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse add book response", e)
                    Log.e(TAG, "Response was: ${response.body}")
                    BooksActionResult.Success("Book added to collection") // Still consider success if response code is 2xx
                }
            }
            else -> {
                val errorMessage = parseError(response.body)
                Log.e(TAG, "Add book failed with code ${response.code}: $errorMessage")
                Log.e(TAG, "Request body was: $body")
                Log.e(TAG, "Response body was: ${response.body}")
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

    sealed class UserBookResult {
        data class Success(val userBook: UserBook) : UserBookResult()
        data class Error(val message: String) : UserBookResult()
    }

    suspend fun getUserBookForBook(bookId: Long, userId: Long): UserBookResult {
        val response = makeAuthenticatedRequest("user_books/by-book/$bookId", "GET")

        return when {
            response.isSuccessful() -> {
                try {
                    Log.d(TAG, "getUserBookForBook - Response: ${response.body}")
                    val json = JSONObject(response.body)

                    if (!json.getBoolean("success")) {
                        return UserBookResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    val userBooks = data.getJSONArray("userbooks")

                    // Find the UserBook for this specific user
                    for (i in 0 until userBooks.length()) {
                        val userBookJson = userBooks.getJSONObject(i)
                        val userBookUserId = userBookJson.getLong("user_id")

                        if (userBookUserId == userId) {
                            // Parse the UserBook from JSON
                            val userBook = parseUserBookFromJson(userBookJson, bookId, userId)
                            Log.d(TAG, "getUserBookForBook - Found UserBook with wishlist_status: ${userBook.wishlistStatus}")
                            return UserBookResult.Success(userBook)
                        }
                    }

                    UserBookResult.Error("UserBook not found for this user")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse getUserBookForBook response", e)
                    Log.e(TAG, "Response was: ${response.body}")
                    UserBookResult.Error("Failed to parse user book data: ${e.message}")
                }
            }
            else -> {
                val errorMessage = parseError(response.body)
                Log.e(TAG, "getUserBookForBook failed: $errorMessage")
                UserBookResult.Error(errorMessage)
            }
        }
    }
    private fun parseUserBookFromJson(json: JSONObject, bookId: Long, userId: Long): UserBook {
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
            personalRating = json.optInt("personal_rating").takeIf { it != 0 },
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