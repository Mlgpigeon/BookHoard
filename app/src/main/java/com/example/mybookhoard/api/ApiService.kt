package com.example.mybookhoard.api

import android.content.Context
import android.content.SharedPreferences
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
import java.net.URLEncoder
import java.net.UnknownHostException

class ApiService(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://api.mybookhoard.com/api"
        private const val PREFS_NAME = "bookhoard_auth"
        private const val TOKEN_KEY = "auth_token"
        private const val TAG = "ApiService"

        // Timeout constants
        private const val CONNECT_TIMEOUT = 10_000 // 10 seconds
        private const val READ_TIMEOUT = 15_000 // 15 seconds
        private const val REQUEST_TIMEOUT = 20_000L // 20 seconds total
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAuthToken(): String? = prefs.getString(TOKEN_KEY, null)

    fun setAuthToken(token: String?) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun clearAuthToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    private suspend fun makeRequest(
        endpoint: String,
        method: String = "GET",
        body: JSONObject? = null,
        requireAuth: Boolean = true
    ): ApiResponse = withContext(Dispatchers.IO) {

        // Wrap entire request with timeout
        val result = withTimeoutOrNull(REQUEST_TIMEOUT) {
            try {
                Log.d(TAG, "Making $method request to: $BASE_URL/$endpoint")

                val url = URL("$BASE_URL/$endpoint")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "BookHoard-Android/1.0")

                    if (requireAuth) {
                        val token = getAuthToken()
                        if (token != null) {
                            setRequestProperty("Authorization", "Bearer $token")
                        }
                    }

                    if (body != null && (method == "POST" || method == "PUT")) {
                        doOutput = true
                        val bodyString = body.toString()
                        Log.d(TAG, "Request body: $bodyString")
                        OutputStreamWriter(outputStream).use {
                            it.write(bodyString)
                        }
                    }
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")

                val responseBody = if (responseCode < 400) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
                }

                Log.d(TAG, "Response body: $responseBody")
                ApiResponse(responseCode, responseBody)

            } catch (e: UnknownHostException) {
                Log.e(TAG, "Network error - unknown host: ${e.message}")
                ApiResponse(0, """{"success": false, "message": "Network error: Unable to connect to server. Please check your internet connection."}""")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Network timeout: ${e.message}")
                ApiResponse(0, """{"success": false, "message": "Network timeout: The request took too long. Please try again."}""")
            } catch (e: Exception) {
                Log.e(TAG, "Request failed: ${e.message}", e)
                ApiResponse(0, """{"success": false, "message": "Connection error: ${e.message ?: "Unknown error"}"}""")
            }
        }

        // Handle timeout case
        result ?: run {
            Log.e(TAG, "Request timed out after ${REQUEST_TIMEOUT}ms")
            ApiResponse(0, """{"success": false, "message": "Request timed out. Please check your internet connection and try again."}""")
        }
    }

    // Auth endpoints
    suspend fun register(username: String, email: String, password: String): AuthResult {
        val body = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }

        val response = makeRequest("auth/register", "POST", body, requireAuth = false)
        return parseAuthResponse(response)
    }

    suspend fun login(identifier: String, password: String): AuthResult {
        val body = JSONObject().apply {
            put("identifier", identifier)
            put("password", password)
        }

        val response = makeRequest("auth/login", "POST", body, requireAuth = false)
        return parseAuthResponse(response)
    }

    suspend fun logout(): ApiResult<Unit> {
        val response = makeRequest("auth/logout", "POST")
        clearAuthToken()
        return if (response.isSuccessful()) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error(parseError(response.body))
        }
    }

    suspend fun getProfile(): ApiResult<User> {
        val response = makeRequest("auth/me")
        return if (response.isSuccessful()) {
            try {
                val json = JSONObject(response.body)
                val userData = json.getJSONObject("data").getJSONObject("user")
                val user = User.fromJson(userData)
                ApiResult.Success(user)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse user data: ${e.message}")
                ApiResult.Error("Failed to parse user data: ${e.message}")
            }
        } else {
            ApiResult.Error(parseError(response.body))
        }
    }

    // Test connectivity
    suspend fun testConnection(): ApiResult<Boolean> {
        return try {
            val response = makeRequest("health", "GET", requireAuth = false)
            if (response.isSuccessful()) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error("Server returned: ${response.code}")
            }
        } catch (e: Exception) {
            ApiResult.Error("Connection test failed: ${e.message}")
        }
    }

    // Book endpoints (to be implemented when API is ready)
    suspend fun getBooks(): ApiResult<List<ApiBook>> {
        val response = makeRequest("books")
        return if (response.isSuccessful()) {
            try {
                val json = JSONObject(response.body)
                val booksArray = json.getJSONObject("data").getJSONArray("books")
                val books = mutableListOf<ApiBook>()
                for (i in 0 until booksArray.length()) {
                    books.add(ApiBook.fromJson(booksArray.getJSONObject(i)))
                }
                ApiResult.Success(books)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse books: ${e.message}")
                ApiResult.Error("Failed to parse books: ${e.message}")
            }
        } else {
            ApiResult.Error(parseError(response.body))
        }
    }

    suspend fun createBook(book: ApiBook): ApiResult<ApiBook> {
        val body = book.toJson()
        val response = makeRequest("books", "POST", body)
        return if (response.isSuccessful()) {
            try {
                val json = JSONObject(response.body)
                val bookData = json.getJSONObject("data").getJSONObject("book")
                ApiResult.Success(ApiBook.fromJson(bookData))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse created book: ${e.message}")
                ApiResult.Error("Failed to parse created book: ${e.message}")
            }
        } else {
            ApiResult.Error(parseError(response.body))
        }
    }

    suspend fun updateBook(id: Long, book: ApiBook): ApiResult<ApiBook> {
        val body = book.toJson()
        val response = makeRequest("books/$id", "PUT", body)
        return if (response.isSuccessful()) {
            try {
                val json = JSONObject(response.body)
                val bookData = json.getJSONObject("data").getJSONObject("book")
                ApiResult.Success(ApiBook.fromJson(bookData))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse updated book: ${e.message}")
                ApiResult.Error("Failed to parse updated book: ${e.message}")
            }
        } else {
            ApiResult.Error(parseError(response.body))
        }
    }

    suspend fun deleteBook(id: Long): ApiResult<Unit> {
        val response = makeRequest("books/$id", "DELETE")
        return if (response.isSuccessful()) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error(parseError(response.body))
        }
    }

    suspend fun searchBooks(query: String): ApiResult<List<ApiBook>> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val response = makeRequest("books/search?q=$encodedQuery")
        return if (response.isSuccessful()) {
            try {
                val json = JSONObject(response.body)
                val booksArray = json.getJSONObject("data").getJSONArray("books")
                val books = mutableListOf<ApiBook>()
                for (i in 0 until booksArray.length()) {
                    books.add(ApiBook.fromJson(booksArray.getJSONObject(i)))
                }
                ApiResult.Success(books)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse search results: ${e.message}")
                ApiResult.Error("Failed to parse search results: ${e.message}")
            }
        } else {
            ApiResult.Error(parseError(response.body))
        }
    }

    private fun parseAuthResponse(response: ApiResponse): AuthResult {
        return if (response.isSuccessful()) {
            try {
                val json = JSONObject(response.body)
                val data = json.getJSONObject("data")
                val token = data.getString("token")
                val userData = data.getJSONObject("user")
                val user = User.fromJson(userData)

                setAuthToken(token)
                AuthResult.Success(user, token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse auth response: ${e.message}")
                AuthResult.Error("Failed to parse authentication response: ${e.message}")
            }
        } else {
            AuthResult.Error(parseError(response.body))
        }
    }

    private fun parseError(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            json.optString("message", "Unknown error occurred")
        } catch (e: Exception) {
            "Unable to connect to server. Please check your internet connection."
        }
    }
}

data class ApiResponse(val code: Int, val body: String) {
    fun isSuccessful(): Boolean = code in 200..299
}

sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String) : ApiResult<T>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = if (this is Success) data else null
    fun errorMessage(): String? = if (this is Error) message else null
}

sealed class AuthResult {
    data class Success(val user: User, val token: String) : AuthResult()
    data class Error(val message: String) : AuthResult()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
}