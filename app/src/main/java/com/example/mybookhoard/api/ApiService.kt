package com.example.mybookhoard.api

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiService(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://api.mybookhoard.com/api"
        private const val PREFS_NAME = "bookhoard_auth"
        private const val TOKEN_KEY = "auth_token"
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
        try {
            val url = URL("$BASE_URL/$endpoint")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")

                if (requireAuth) {
                    val token = getAuthToken()
                    if (token != null) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }

                if (body != null && (method == "POST" || method == "PUT")) {
                    doOutput = true
                    OutputStreamWriter(outputStream).use { it.write(body.toString()) }
                }
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode < 400) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            }

            ApiResponse(responseCode, responseBody)

        } catch (e: Exception) {
            ApiResponse(0, """{"success": false, "message": "${e.message}"}""")
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
                ApiResult.Error("Failed to parse user data: ${e.message}")
            }
        } else {
            ApiResult.Error(parseError(response.body))
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
                AuthResult.Error("Failed to parse auth response: ${e.message}")
            }
        } else {
            AuthResult.Error(parseError(response.body))
        }
    }

    private fun parseError(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            json.getString("message")
        } catch (e: Exception) {
            "Unknown error occurred"
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