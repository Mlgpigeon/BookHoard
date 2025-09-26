package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.BookSource
import com.example.mybookhoard.data.entities.UserBook
import com.example.mybookhoard.data.entities.UserBookReadingStatus
import com.example.mybookhoard.data.entities.UserBookWishlistStatus
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

    suspend fun updateUserBookStatus(
        userBookId: Long,
        newReading: UserBookReadingStatus?,
        newWishlist: UserBookWishlistStatus?
         ): UserBookResult {
         return try {
             val payload = JSONObject().apply {
                 newReading?.let {
                     put(
                         "reading_status",
                         when (it) {
                             UserBookReadingStatus.NOT_STARTED -> "not_started"
                             UserBookReadingStatus.READING -> "reading"
                             UserBookReadingStatus.READ -> "read"
                             UserBookReadingStatus.ABANDONED -> "abandoned"
                         }
                                 )
                     }
                 newWishlist?.let {
                     put(
                         "wishlist_status",
                         when (it) {
                             UserBookWishlistStatus.WISH -> "wish"
                             UserBookWishlistStatus.ON_THE_WAY -> "on_the_way"
                             UserBookWishlistStatus.OBTAINED -> "obtained"
                             }
                                 )
                     }
                 }

             val response = apiClient.makeAuthenticatedRequest(
                 "user_books/$userBookId",
                 "PUT",
                 payload
                         )

             if (response.isSuccessful()) {
                 val root = JSONObject(response.body)
                 val data = if (root.has("data")) root.get("data") else root
                 val obj = when (data) {
                     is JSONObject -> data
                     is JSONArray -> if (data.length() > 0) data.getJSONObject(0) else JSONObject()
                     else -> JSONObject()
                     }

                 // Necesitamos bookId y userId para el parser
                 val bookId = obj.optLong("book_id")
                 val userId = obj.optLong("user_id")
                 val parsed = UserBookParser.parseUserBookFromJson(obj, bookId, userId)
                 UserBookResult.Success(parsed)
                 } else {
                 UserBookResult.Error("HTTP ${'$'}{response.code}: ${'$'}{apiClient.extractErrorMessage(response.body)}")
                 }
             } catch (e: Exception) {
             Log.e(TAG, "updateUserBookStatus - exception", e)
             UserBookResult.Error(e.message ?: "Unknown error")
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

    suspend fun getUserBooksForUser(userId: Long): List<UserBook> {
        val response = apiClient.makeAuthenticatedRequest("user_books?user_id=$userId", "GET")
        return if (response.isSuccessful()) {
            try {
                val root = JSONObject(response.body)
                val arr = root.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<UserBook>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val ub = UserBookParser.parseUserBookFromJson(
                        obj,
                        obj.optLong("book_id"),
                        obj.optLong("user_id")
                    )
                    if (ub.userId == userId) list.add(ub)
                }
                list
            } catch (e: Exception) {
                Log.e(TAG, "getUserBooksForUser - parse error", e)
                emptyList()
            }
        } else {
            Log.w(TAG, "getUserBooksForUser - request failed ${'$'}{response.code}")
            emptyList()
        }
    }

    suspend fun getBookById(bookId: Long): ApiBook? {
         val response = apiClient.makeAuthenticatedRequest("books/$bookId", "GET")
         return if (response.isSuccessful()) {
             try {
                 val root = JSONObject(response.body)
                 // Soportar tanto formato envuelto como objeto directo
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
             Log.w(TAG, "getBookById - request failed ${'$'}{response.code}")
             null }
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
    suspend fun getUserBooksWithDetails(userId: Long): LibraryResult {
        val response = apiClient.makeAuthenticatedRequest("user_books/with-details/$userId", "GET")

        return when {
            response.isSuccessful() -> {
                try {
                    Log.d(TAG, "getUserBooksWithDetails - Response: ${response.body}")
                    val json = JSONObject(response.body)

                    if (!json.getBoolean("success")) {
                        return LibraryResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    val userBooksArray = data.getJSONArray("user_books_with_details")
                    val totalCount = data.getInt("total_count")

                    val libraryItems = mutableListOf<LibraryItem>()

                    for (i in 0 until userBooksArray.length()) {
                        try {
                            val item = userBooksArray.getJSONObject(i)
                            val userBookJson = item.getJSONObject("userbook")
                            val bookJson = item.getJSONObject("book")

                            // Parse UserBook
                            val userBook = UserBookParser.parseUserBookFromJson(
                                userBookJson,
                                bookJson.getLong("id"),
                                userBookJson.getLong("user_id")
                            )

                            // Parse Book with author info included
                            val book = parseBookFromDetailedJson(bookJson)

                            libraryItems.add(
                                LibraryItem(
                                    book = book,
                                    userBook = userBook,
                                    authorName = bookJson.optString("author").takeIf { it.isNotBlank() }
                                )
                            )

                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse library item at index $i: ${e.message}")
                        }
                    }

                    Log.d(TAG, "Successfully loaded ${libraryItems.size} library items")
                    LibraryResult.Success(libraryItems, totalCount)

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse getUserBooksWithDetails response", e)
                    LibraryResult.Error("Failed to parse library data: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "getUserBooksWithDetails failed: $errorMessage")
                LibraryResult.Error(errorMessage)
            }
        }
    }
    private fun parseBookFromDetailedJson(bookJson: JSONObject): Book {
        return Book(
            id = bookJson.getLong("id"),
            title = bookJson.getString("title"),
            originalTitle = bookJson.optString("original_title").takeIf { it.isNotBlank() },
            description = bookJson.optString("description").takeIf { it.isNotBlank() },
            primaryAuthorId = bookJson.optLong("primary_author_id", 0).takeIf { it != 0L },
            sagaId = bookJson.optLong("saga_id", 0).takeIf { it != 0L },
            sagaNumber = bookJson.optInt("saga_number", 0).takeIf { it != 0 },
            language = bookJson.optString("language", "en"),
            publicationYear = bookJson.optInt("publication_year", 0).takeIf { it != 0 },
            genres = bookJson.optJSONArray("genres")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            isbn = bookJson.optString("isbn").takeIf { it.isNotBlank() },
            coverSelected = bookJson.optString("cover_selected").takeIf { it.isNotBlank() },
            images = bookJson.optJSONArray("images")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            adaptations = bookJson.optJSONArray("adaptations")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            },
            averageRating = bookJson.optDouble("average_rating", 0.0).toFloat(),
            totalRatings = bookJson.optInt("total_ratings", 0),
            isPublic = bookJson.optBoolean("is_public", true),
            source = when (bookJson.optString("source", "user_defined")) {
                "google_books_api" -> BookSource.GOOGLE_BOOKS_API
                "openlibrary_api" -> BookSource.OPENLIBRARY_API
                else -> BookSource.USER_DEFINED
            },
            createdAt = java.util.Date(),
            updatedAt = java.util.Date()
        )
    }


}