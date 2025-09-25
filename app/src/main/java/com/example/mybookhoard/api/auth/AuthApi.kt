package com.example.mybookhoard.api.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.mybookhoard.data.auth.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

data class ApiResponse(val code: Int, val body: String) {
    fun isSuccessful(): Boolean = code in 200..299
}

sealed class AuthResult {
    data class Success(val user: User, val token: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthApi(private val context: Context) {
    companion object {
        private const val BASE_URL = "https://api.mybookhoard.com/api"
        private const val PREFS_NAME = "bookhoard_auth"
        private const val TOKEN_KEY = "auth_token"
        private const val USER_ID_KEY = "user_id"
        private const val USERNAME_KEY = "username"
        private const val EMAIL_KEY = "email"
        private const val ROLE_KEY = "role"
        private const val CREATED_AT_KEY = "created_at"
        private const val IS_ACTIVE_KEY = "is_active"
        private const val LAST_SYNC_KEY = "last_sync"
        private const val TAG = "AuthApi"

        private const val CONNECT_TIMEOUT = 10_000
        private const val READ_TIMEOUT = 15_000
        private const val REQUEST_TIMEOUT = 20_000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun login(identifier: String, password: String): AuthResult {
        val body = JSONObject().apply {
            put("identifier", identifier)
            put("password", password)
        }

        val response = makeRequest("auth/login", "POST", body, requireAuth = false)
        return parseAuthResponse(response)
    }

    suspend fun register(username: String, email: String, password: String): AuthResult {
        val body = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }

        val response = makeRequest("auth/register", "POST", body, requireAuth = false)
        return parseAuthResponse(response)
    }

    fun getAuthToken(): String? = prefs.getString(TOKEN_KEY, null)

    fun clearUserSession() {
        prefs.edit().clear().apply()
    }

    // NEW: Check if there's a saved session
    fun hasSavedSession(): Boolean {
        val token = prefs.getString(TOKEN_KEY, null)
        val userId = prefs.getLong(USER_ID_KEY, -1)
        val username = prefs.getString(USERNAME_KEY, null)
        return token != null && userId != -1L && username != null
    }

    // NEW: Get saved session data
    fun getSavedSession(): AuthResult? {
        if (!hasSavedSession()) return null

        val token = prefs.getString(TOKEN_KEY, null) ?: return null
        val user = getSavedUser() ?: return null

        Log.d(TAG, "Retrieved saved session for user: ${user.username}")
        return AuthResult.Success(user, token)
    }

    // NEW: Get saved user data
    private fun getSavedUser(): User? {
        return try {
            val id = prefs.getLong(USER_ID_KEY, -1)
            if (id == -1L) return null

            User(
                id = id,
                username = prefs.getString(USERNAME_KEY, "") ?: "",
                email = prefs.getString(EMAIL_KEY, "") ?: "",
                role = prefs.getString(ROLE_KEY, "user") ?: "user",
                createdAt = prefs.getString(CREATED_AT_KEY, "") ?: "",
                isActive = prefs.getBoolean(IS_ACTIVE_KEY, true)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving saved user: ${e.message}")
            null
        }
    }

    private fun saveUser(user: User, token: String) {
        prefs.edit().apply {
            putString(TOKEN_KEY, token)
            putLong(USER_ID_KEY, user.id)
            putString(USERNAME_KEY, user.username)
            putString(EMAIL_KEY, user.email)
            putString(ROLE_KEY, user.role)
            putString(CREATED_AT_KEY, user.createdAt)
            putBoolean(IS_ACTIVE_KEY, user.isActive)
            putLong(LAST_SYNC_KEY, System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "User session saved: ${user.username} (ID: ${user.id})")
    }

    private fun parseAuthResponse(response: ApiResponse): AuthResult {
        return if (response.isSuccessful()) {
            try {
                val json = JSONObject(response.body)
                val data = json.getJSONObject("data")
                val token = data.getString("token")
                val userData = data.getJSONObject("user")
                val user = User.fromJson(userData)

                saveUser(user, token)
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
            "Unable to connect to server."
        }
    }

    // --- REQUEST HELPER ---
    private suspend fun makeRequest(
        endpoint: String,
        method: String,
        body: JSONObject? = null,
        requireAuth: Boolean = true
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

                    if (requireAuth) {
                        val token = getAuthToken()
                        if (token != null) {
                            setRequestProperty("Authorization", "Bearer $token")
                        }
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
}