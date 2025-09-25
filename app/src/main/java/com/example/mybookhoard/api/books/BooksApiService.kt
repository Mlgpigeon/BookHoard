package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for interacting with BookHoard API books endpoints
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
     * Add book to user's collection via API
     */
    suspend fun addBookToCollection(bookId: Long, wishlistStatus: String): BooksActionResult {
        val currentUserId = apiClient.getCurrentUserId()
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

        Log.d(TAG, "addBookToCollection - Sending request: $body")

        val response = apiClient.makeAuthenticatedRequest("user_books", "POST", body)

        return when {
            response.isSuccessful() -> {
                Log.d(TAG, "addBookToCollection - Response: ${response.body}")
                try {
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
                    BooksActionResult.Success("Book added to collection")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Add book failed with code ${response.code}: $errorMessage")
                BooksActionResult.Error(errorMessage)
            }
        }
    }

    /**
     * Remove book from user's collection via API
     */
    suspend fun removeBookFromCollection(bookId: Long): BooksActionResult {
        val currentUserId = apiClient.getCurrentUserId()
        if (currentUserId == -1L) {
            return BooksActionResult.Error("User not authenticated")
        }

        val getUserBookResponse = apiClient.makeAuthenticatedRequest("user_books/by-book/$bookId", "GET")

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

            val deleteResponse = apiClient.makeAuthenticatedRequest("user_books/$userBookId", "DELETE")

            return when {
                deleteResponse.isSuccessful() -> {
                    Log.d(TAG, "Book removed from collection successfully")
                    BooksActionResult.Success("Book removed from collection")
                }
                else -> {
                    val errorMessage = apiClient.parseError(deleteResponse.body)
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
     * Get user book for specific book and user
     */
    suspend fun getUserBookForBook(bookId: Long, userId: Long): UserBookResult {
        val response = apiClient.makeAuthenticatedRequest("user_books/by-book/$bookId", "GET")

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

                    for (i in 0 until userBooks.length()) {
                        val userBookJson = userBooks.getJSONObject(i)
                        val userBookUserId = userBookJson.getLong("user_id")

                        if (userBookUserId == userId) {
                            val userBook = UserBookParser.parseUserBookFromJson(userBookJson, bookId, userId)
                            Log.d(TAG, "getUserBookForBook - Found UserBook with wishlist_status: ${userBook.wishlistStatus}")
                            return UserBookResult.Success(userBook)
                        }
                    }

                    UserBookResult.Error("UserBook not found for this user")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse getUserBookForBook response", e)
                    UserBookResult.Error("Failed to parse user book data: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "getUserBookForBook failed: $errorMessage")
                UserBookResult.Error(errorMessage)
            }
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