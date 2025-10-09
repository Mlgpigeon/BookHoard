package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
import com.example.mybookhoard.api.auth.AuthApi
import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.BookSource
import com.example.mybookhoard.data.entities.Saga
import org.json.JSONArray
import org.json.JSONObject

/**
 * API Service for Saga/Series management
 */
class SagasApiService(
    private val context: Context,
    private val authApi: AuthApi
) {
    companion object {
        private const val TAG = "SagasApiService"
    }

    private val apiClient = BooksApiClient(context, authApi)

    suspend fun getAllSagas(): SagasResult {
        Log.d(TAG, "=== getAllSagas() called ===")
        val response = apiClient.makeAuthenticatedRequest("sagas", "GET")

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: ${response.body}")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)
                    Log.d(TAG, "Parsed JSON: $json")

                    if (!json.getBoolean("success")) {
                        Log.e(TAG, "API returned success=false")
                        return SagasResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    Log.d(TAG, "Data object: $data")

                    val sagasArray = data.getJSONArray("sagas")
                    Log.d(TAG, "Sagas array length: ${sagasArray.length()}")

                    val sagas = mutableListOf<Saga>()
                    for (i in 0 until sagasArray.length()) {
                        val sagaJson = sagasArray.getJSONObject(i)
                        Log.d(TAG, "Parsing saga $i: $sagaJson")
                        sagas.add(Saga.fromJson(sagaJson))
                    }

                    Log.d(TAG, "Successfully loaded ${sagas.size} sagas")
                    SagasResult.Success(sagas)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse sagas", e)
                    Log.e(TAG, "Response body was: ${response.body}")
                    SagasResult.Error("Failed to parse sagas: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Get sagas failed with code ${response.code}")
                Log.e(TAG, "Error message: $errorMessage")
                Log.e(TAG, "Full response body: ${response.body}")
                SagasResult.Error(errorMessage)
            }
        }
    }

    suspend fun getSaga(sagaId: Long): SagaResult {
        Log.d(TAG, "=== getSaga($sagaId) called ===")
        val response = apiClient.makeAuthenticatedRequest("sagas/$sagaId", "GET")

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: ${response.body}")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)

                    if (!json.getBoolean("success")) {
                        Log.e(TAG, "API returned success=false")
                        return SagaResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    val saga = Saga.fromJson(data)

                    Log.d(TAG, "Loaded saga: ${saga.name}")
                    SagaResult.Success(saga)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse saga", e)
                    Log.e(TAG, "Response body was: ${response.body}")
                    SagaResult.Error("Failed to parse saga: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Get saga failed: $errorMessage")
                SagaResult.Error(errorMessage)
            }
        }
    }

    suspend fun createSaga(
        name: String,
        description: String?,
        primaryAuthorId: Long?,
        totalBooks: Int,
        isCompleted: Boolean
    ): SagaResult {
        val body = JSONObject().apply {
            put("name", name.trim())
            description?.let { put("description", it.trim()) }
            primaryAuthorId?.let { put("primary_author_id", it) }
            put("total_books", totalBooks)
            put("is_completed", isCompleted)
        }

        Log.d(TAG, "=== createSaga() called ===")
        Log.d(TAG, "Request body: $body")

        val response = apiClient.makeAuthenticatedRequest("sagas", "POST", body)

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: ${response.body}")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)

                    if (!json.getBoolean("success")) {
                        Log.e(TAG, "API returned success=false")
                        return SagaResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    val saga = Saga.fromJson(data)

                    Log.d(TAG, "Created saga: ${saga.name} with id: ${saga.id}")
                    SagaResult.Success(saga)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse created saga", e)
                    Log.e(TAG, "Response body was: ${response.body}")
                    SagaResult.Error("Failed to parse saga: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Create saga failed with code: ${response.code}")
                Log.e(TAG, "Error message: $errorMessage")
                Log.e(TAG, "Full response body: ${response.body}")
                SagaResult.Error(errorMessage)
            }
        }
    }

    suspend fun updateSaga(
        sagaId: Long,
        name: String?,
        description: String?,
        primaryAuthorId: Long?,
        totalBooks: Int?,
        isCompleted: Boolean?
    ): SagaResult {
        val body = JSONObject().apply {
            name?.let { put("name", it.trim()) }
            description?.let { put("description", it.trim()) }
            primaryAuthorId?.let { put("primary_author_id", it) }
            totalBooks?.let { put("total_books", it) }
            isCompleted?.let { put("is_completed", it) }
        }

        Log.d(TAG, "=== updateSaga($sagaId) called ===")
        Log.d(TAG, "Request body: $body")

        val response = apiClient.makeAuthenticatedRequest("sagas/$sagaId", "PUT", body)

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: ${response.body}")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)

                    if (!json.getBoolean("success")) {
                        return SagaResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    val saga = Saga.fromJson(data)

                    Log.d(TAG, "Updated saga: ${saga.name}")
                    SagaResult.Success(saga)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse updated saga", e)
                    SagaResult.Error("Failed to parse saga: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Update saga failed: $errorMessage")
                SagaResult.Error(errorMessage)
            }
        }
    }

    suspend fun deleteSaga(sagaId: Long): ActionResult {
        Log.d(TAG, "=== deleteSaga($sagaId) called ===")
        val response = apiClient.makeAuthenticatedRequest("sagas/$sagaId", "DELETE")

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: ${response.body}")

        return when {
            response.isSuccessful() -> {
                Log.d(TAG, "Deleted saga $sagaId")
                ActionResult.Success("Saga deleted successfully")
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Delete saga failed: $errorMessage")
                ActionResult.Error(errorMessage)
            }
        }
    }

    suspend fun getBooksInSaga(sagaId: Long): BooksResult {
        Log.d(TAG, "=== getBooksInSaga($sagaId) called ===")
        val response = apiClient.makeAuthenticatedRequest("sagas/books/$sagaId", "GET")

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: ${response.body}")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)

                    if (!json.getBoolean("success")) {
                        return BooksResult.Error("API returned unsuccessful response")
                    }

                    val data = json.getJSONObject("data")
                    val booksArray = data.getJSONArray("books")

                    val books = mutableListOf<Book>()
                    for (i in 0 until booksArray.length()) {
                        val bookJson = booksArray.getJSONObject(i)
                        books.add(parseBookFromJson(bookJson))
                    }

                    Log.d(TAG, "Loaded ${books.size} books for saga $sagaId")
                    BooksResult.Success(books)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse books in saga", e)
                    Log.e(TAG, "Response body was: ${response.body}")
                    BooksResult.Error("Failed to parse books: ${e.message}")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Get books in saga failed: $errorMessage")
                BooksResult.Error(errorMessage)
            }
        }
    }

    suspend fun updateBooksOrder(sagaId: Long, bookOrders: Map<Long, Int>): ActionResult {
        val body = JSONObject().apply {
            val ordersJson = JSONObject()
            bookOrders.forEach { (bookId, order) ->
                ordersJson.put(bookId.toString(), order)
            }
            put("book_orders", ordersJson)
        }

        Log.d(TAG, "=== updateBooksOrder($sagaId) called ===")
        Log.d(TAG, "Book IDs being sent: ${bookOrders.keys.toList()}")
        Log.d(TAG, "Book orders: $bookOrders")
        Log.d(TAG, "Request body: $body")

        val response = apiClient.makeAuthenticatedRequest("sagas/order/$sagaId", "POST", body)

        Log.d(TAG, "Response code: ${response.code}")
        Log.d(TAG, "Response body: ${response.body}")

        return when {
            response.isSuccessful() -> {
                try {
                    val json = JSONObject(response.body)

                    // Log debug info if available
                    if (json.has("data") && json.getJSONObject("data").has("debug")) {
                        val debugInfo = json.getJSONObject("data").getJSONObject("debug")
                        Log.d(TAG, "=== DEBUG INFO FROM SERVER ===")
                        Log.d(TAG, "Saga ID: ${debugInfo.optLong("saga_id")}")
                        Log.d(TAG, "Books found in DB: ${debugInfo.optInt("books_found_count")}")
                        Log.d(TAG, "Books updated: ${debugInfo.optInt("books_updated")}")
                        Log.d(TAG, "Books not found: ${debugInfo.optJSONArray("books_not_found")}")
                        Log.d(TAG, "Transaction committed: ${debugInfo.optBoolean("transaction_committed")}")

                        // Log detailed info about each book
                        if (debugInfo.has("books_processed")) {
                            val booksProcessed = debugInfo.getJSONArray("books_processed")
                            Log.d(TAG, "Books processed details:")
                            for (i in 0 until booksProcessed.length()) {
                                val bookInfo = booksProcessed.getJSONObject(i)
                                Log.d(TAG, "  - Book ${bookInfo.optLong("book_id")}: " +
                                        "order=${bookInfo.optInt("order")}, " +
                                        "rows_affected=${bookInfo.optInt("rows_affected")}")
                            }
                        }

                        // Log books found in DB
                        if (debugInfo.has("books_found_in_db")) {
                            val booksFound = debugInfo.getJSONArray("books_found_in_db")
                            Log.d(TAG, "Books found in database:")
                            for (i in 0 until booksFound.length()) {
                                val book = booksFound.getJSONObject(i)
                                Log.d(TAG, "  - ID: ${book.optLong("id")}, " +
                                        "Title: ${book.optString("title")}, " +
                                        "Current saga_id: ${book.optString("saga_id")}, " +
                                        "Current saga_number: ${book.optString("saga_number")}")
                            }
                        }
                        Log.d(TAG, "=== END DEBUG INFO ===")
                    }

                    Log.d(TAG, "Updated books order for saga $sagaId")
                    ActionResult.Success("Books order updated successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse response", e)
                    ActionResult.Success("Books order updated (parse warning)")
                }
            }
            else -> {
                val errorMessage = apiClient.parseError(response.body)
                Log.e(TAG, "Update books order failed: $errorMessage")
                ActionResult.Error(errorMessage)
            }
        }
    }

    private fun parseBookFromJson(json: JSONObject): Book {
        return Book(
            id = json.getLong("id"),
            title = json.getString("title"),
            originalTitle = json.optString("original_title").takeIf { it.isNotBlank() },
            description = json.optString("description").takeIf { it.isNotBlank() },
            primaryAuthorId = json.optLong("primary_author_id", 0).takeIf { it != 0L },
            sagaId = json.optLong("saga_id", 0).takeIf { it != 0L },
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
            source = when (json.optString("source", "user_defined")) {
                "google_books_api" -> BookSource.GOOGLE_BOOKS_API
                "openlibrary_api" -> BookSource.OPENLIBRARY_API
                else -> BookSource.USER_DEFINED
            }
        )
    }
}

// Result classes
sealed class SagasResult {
    data class Success(val sagas: List<Saga>) : SagasResult()
    data class Error(val message: String) : SagasResult()
}

sealed class SagaResult {
    data class Success(val saga: Saga) : SagaResult()
    data class Error(val message: String) : SagaResult()
}

sealed class BooksResult {
    data class Success(val books: List<Book>) : BooksResult()
    data class Error(val message: String) : BooksResult()
}

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Error(val message: String) : ActionResult()
}