package com.example.mybookhoard.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.mybookhoard.data.User
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

    private fun saveUser(user: User, token: String) {
        prefs.edit().apply {
            putString(TOKEN_KEY, token)
            putLong("user_id", user.id)
            putString("username", user.username)
            putString("email", user.email)
            putString("role", user.role)
            putString("created_at", user.createdAt)
            putBoolean("is_active", user.isActive)
            putLong("last_sync", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "User session saved: ${user.username}")
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
        val result = withTimeoutOrNull(REQUEST_TIMEOUT) {
            try {
                val url = URL("$BASE_URL/$endpoint")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "BookHoard-Android/1.0")

                    if (requireAuth) {
                        getAuthToken()?.let {
                            setRequestProperty("Authorization", "Bearer $it")
                        }
                    }

                    if (body != null && (method == "POST" || method == "PUT")) {
                        doOutput = true
                        val bodyString = body.toString()
                        Log.d(TAG, "Request body: $bodyString")
                        OutputStreamWriter(outputStream).use { it.write(bodyString) }
                    }
                }

                val code = conn.responseCode
                val resp = if (code < 400) {
                    BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                }

                Log.d(TAG, "Response ($code): $resp")
                ApiResponse(code, resp)

            } catch (e: UnknownHostException) {
                ApiResponse(0, """{"success": false, "message": "Network error: unknown host"}""")
            } catch (e: SocketTimeoutException) {
                ApiResponse(0, """{"success": false, "message": "Network timeout"}""")
            } catch (e: Exception) {
                ApiResponse(0, """{"success": false, "message": "Error: ${e.message}"}""")
            }
        }

        result ?: ApiResponse(0, """{"success": false, "message": "Request timed out"}""")
    }
}
