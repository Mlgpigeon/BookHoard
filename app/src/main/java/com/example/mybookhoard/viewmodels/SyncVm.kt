package com.example.mybookhoard.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.SyncResult
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.ReadingStatus
import com.example.mybookhoard.data.WishlistStatus
import com.example.mybookhoard.repository.BookRepository
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SyncVm(private val repository: BookRepository) : ViewModel() {

    companion object {
        private const val TAG = "SyncVm"
    }

    // Sync operations with retry logic
    fun syncFromServer() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting sync from server")
                val result = repository.syncFromServer()
                when (result) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "Sync from server successful")
                    }
                    is SyncResult.Error -> {
                        Log.e(TAG, "Sync from server failed: ${result.message}")
                    }
                    is SyncResult.Partial -> {
                        Log.w(TAG, "Sync from server partial: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync from server error: ${e.message}", e)
            }
        }
    }

    fun syncToServer() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting sync to server")
                val result = repository.syncToServer()
                when (result) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "Sync to server successful")
                    }
                    is SyncResult.Error -> {
                        Log.e(TAG, "Sync to server failed: ${result.message}")
                    }
                    is SyncResult.Partial -> {
                        Log.w(TAG, "Sync to server partial: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync to server error: ${e.message}", e)
            }
        }
    }

    // Bulk operations
    fun replaceAll(list: List<Book>) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Replacing all books with ${list.size} books")
                repository.replaceAllBooks(list)
                Log.d(TAG, "All books replaced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error replacing all books: ${e.message}", e)
            }
        }
    }

    // Import from assets (only for first run or offline mode)
    fun importFromAssetsOnce(ctx: Context, authVm: AuthVm) {
        viewModelScope.launch {
            try {
                // Only import if not authenticated and no local data
                if (!authVm.isAuthenticated()) {
                    val current = repository.getAllBooks().firstOrNull() ?: emptyList()
                    if (current.isEmpty()) {
                        Log.d(TAG, "Importing initial data from assets")
                        val csv = ctx.assets.open("libros_iniciales.csv")
                            .bufferedReader().use { it.readText() }
                        val books = parseCsv(csv)
                        repository.replaceAllBooks(books)
                        Log.d(TAG, "Imported ${books.size} books from assets")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing from assets: ${e.message}", e)
            }
        }
    }

    // CSV parsing (kept for local imports)
    private fun parseCsv(csv: String): List<Book> {
        return try {
            val reader = csvReader {
                skipEmptyLine = true
                autoRenameDuplicateHeaders = true
            }
            val rows = reader.readAllWithHeader(csv.byteInputStream())

            rows.mapNotNull { r ->
                try {
                    val title = r["Title"]?.trim().orEmpty()
                    if (title.isBlank()) return@mapNotNull null

                    val readStr = r["Read"]?.trim()?.lowercase().orEmpty()
                    val saga = r["Saga"]?.trim().orEmpty().ifBlank { null }
                    val author = r["Author"]?.trim().orEmpty().ifBlank { null }
                    val description = r["Description"]?.trim().orEmpty().ifBlank { null }

                    val status = when (readStr) {
                        "leyendo", "reading" -> ReadingStatus.READING
                        "read", "true", "1", "sí", "si", "x", "✓", "✔" -> ReadingStatus.READ
                        else -> ReadingStatus.NOT_STARTED
                    }

                    val wishlistStr = r["Wishlist"]?.trim()?.uppercase().orEmpty()
                    val wishlistStatus = when (wishlistStr) {
                        "WISH" -> WishlistStatus.WISH
                        "ON_THE_WAY" -> WishlistStatus.ON_THE_WAY
                        "OBTAINED" -> WishlistStatus.OBTAINED
                        else -> null
                    }

                    Book(
                        title = title,
                        author = author,
                        saga = saga,
                        description = description,
                        status = status,
                        wishlist = wishlistStatus
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing CSV row: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CSV: ${e.message}", e)
            emptyList()
        }
    }
}