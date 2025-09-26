package com.example.mybookhoard.api.books

import android.content.Context
import android.util.Log
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

/**
 * Base HTTP client for Books API requests
 * Path: app/src/main/java/com/example/mybookhoard/api/books/BooksApiClient.kt
 */
class BooksApiClient(private val context: Context) {
    companion object {
        private const val BASE_URL = "https://api.mybookhoard.com/api"
        private const val TAG = "BooksApiClient"

        private const val CONNECT_TIMEOUT = 10_000
        private const val READ_TIMEOUT = 15_000
        private const val REQUEST_TIMEOUT = 20_000L
    }
    fun extractErrorMessage(responseBody: String): String {
        return parseError(responseBody)
    }
    /**
     * Make authenticated request to the API
     */
    suspend fun makeAuthenticatedRequest(
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
     * Parse error message from response body
     */
    fun parseError(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            json.optString("error", json.optString("message", "Unknown error occurred"))
        } catch (e: Exception) {
            "Unable to connect to server."
        }
    }

    /**
     * Get current user ID from shared preferences
     */
    fun getCurrentUserId(): Long {
        val prefs = context.getSharedPreferences("bookhoard_auth", Context.MODE_PRIVATE)
        return prefs.getLong("user_id", -1)
    }
}