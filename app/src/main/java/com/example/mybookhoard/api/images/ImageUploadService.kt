package com.example.mybookhoard.api

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mybookhoard.api.auth.AuthApi
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for uploading book cover images to the API
 * Uses HttpURLConnection for consistency with existing codebase
 */
class ImageUploadService(
    private val context: Context,
    private val authApi: AuthApi
) {
    companion object {
        private const val TAG = "ImageUploadService"
        private const val BASE_URL = "https://api.mybookhoard.com/api"
        private const val CONNECT_TIMEOUT = 30_000
        private const val READ_TIMEOUT = 30_000
        private const val LINE_END = "\r\n"
        private const val TWO_HYPHENS = "--"
        private const val BOUNDARY_PREFIX = "*****"
    }

    private fun generateBoundary(): String {
        return "$BOUNDARY_PREFIX${System.currentTimeMillis()}$BOUNDARY_PREFIX"
    }

    /**
     * Upload an image and return the URL
     */
    suspend fun uploadBookCover(imageUri: Uri): ImageUploadResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var outputStream: DataOutputStream? = null
        var inputStream: InputStream? = null

        try {
            // Get JWT token
            val token = authApi.getAuthToken()
            if (token.isNullOrBlank()) {
                return@withContext ImageUploadResult.Error("Authentication required")
            }

            // Generate boundary for this request
            val boundary = generateBoundary()

            // Setup connection
            val url = URL("$BASE_URL/books/upload-cover")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                useCaches = false
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT

                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Authorization", "Bearer $token")
            }

            outputStream = DataOutputStream(connection.outputStream)

            // Read file from URI
            val fileInputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext ImageUploadResult.Error("Failed to read image file")

            val fileName = "book_cover_${System.currentTimeMillis()}.jpg"

            // Write multipart form data
            outputStream.apply {
                writeBytes("$TWO_HYPHENS$boundary$LINE_END")
                writeBytes("Content-Disposition: form-data; name=\"cover_image\"; filename=\"$fileName\"$LINE_END")
                writeBytes("Content-Type: image/jpeg$LINE_END")
                writeBytes(LINE_END)

                // Copy file data
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    write(buffer, 0, bytesRead)
                }

                writeBytes(LINE_END)
                writeBytes("$TWO_HYPHENS$boundary$TWO_HYPHENS$LINE_END")
                flush()
            }

            fileInputStream.close()
            outputStream.close()

            // Read response
            val responseCode = connection.responseCode

            inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode in 200..299) {
                val json = JSONObject(responseBody)

                // Check if response has success flag
                if (!json.optBoolean("success", false)) {
                    Log.e(TAG, "API returned success=false")
                    return@withContext ImageUploadResult.Error("Upload failed: API returned unsuccessful response")
                }

                // Get data object and extract URL
                val data = json.optJSONObject("data")
                val imageUrl = data?.optString("url") ?: ""

                if (imageUrl.isNotBlank()) {
                    Log.d(TAG, "Image uploaded successfully: $imageUrl")
                    ImageUploadResult.Success(imageUrl)
                } else {
                    Log.e(TAG, "No URL in response data")
                    ImageUploadResult.Error("Invalid server response: missing URL in data")
                }
            } else {
                val errorMsg = try {
                    JSONObject(responseBody).optString("message", "Upload failed")
                } catch (e: Exception) {
                    "Upload failed"
                }

                Log.e(TAG, "Upload failed: $errorMsg ($responseCode)")
                ImageUploadResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading image", e)
            ImageUploadResult.Error(e.message ?: "Unknown error")
        } finally {
            outputStream?.close()
            inputStream?.close()
            connection?.disconnect()
        }
    }

    /**
     * Result of image upload operation
     */
    sealed class ImageUploadResult {
        data class Success(val url: String) : ImageUploadResult()
        data class Error(val message: String) : ImageUploadResult()
    }
}