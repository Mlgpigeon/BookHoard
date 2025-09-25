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

    // FIXED: Bulk operations using the new replaceAllBooks method
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
                        Log.d(TAG, "No local books found, importing from assets...")
                        importFromAssets(ctx)
                    } else {
                        Log.d(TAG, "Local books exist (${current.size}), skipping asset import")
                    }
                } else {
                    Log.d(TAG, "User authenticated, skipping asset import")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in importFromAssetsOnce: ${e.message}", e)
            }
        }
    }

    // FIXED: Added missing importFromAssets method
    private suspend fun importFromAssets(ctx: Context) {
        try {
            val inputStream = ctx.assets.open("libros_iniciales.csv")
            val csvData = csvReader {
                delimiter = ','
                skipEmptyLine = true
                escapeChar = '"'
            }.readAllWithHeader(inputStream)

            Log.d(TAG, "CSV loaded with ${csvData.size} rows")

            val books = csvData.mapNotNull { row ->
                try {
                    val title = row["title"]?.trim()
                    val author = row["author"]?.trim()
                    val status = row["status"]?.trim()
                    val wishlistStr = row["wishlist"]?.trim()

                    if (title.isNullOrBlank()) {
                        Log.w(TAG, "Skipping row with empty title: $row")
                        return@mapNotNull null
                    }

                    val readingStatus = when (status?.uppercase()) {
                        "READ", "LEIDO", "LEÍDO" -> ReadingStatus.READ
                        "READING", "LEYENDO" -> ReadingStatus.READING
                        else -> ReadingStatus.NOT_STARTED
                    }

                    val wishlistStatus = when (wishlistStr?.uppercase()) {
                        "WISH", "DESEO" -> WishlistStatus.WISH
                        "ON_THE_WAY", "EN_CAMINO" -> WishlistStatus.ON_THE_WAY
                        "OBTAINED", "OBTENIDO" -> WishlistStatus.OBTAINED
                        else -> null
                    }

                    Book(
                        title = title,
                        author = author.takeIf { !it.isNullOrBlank() },
                        saga = row["saga"]?.trim().takeIf { !it.isNullOrBlank() },
                        description = row["description"]?.trim().takeIf { !it.isNullOrBlank() },
                        status = readingStatus,
                        wishlist = wishlistStatus
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing CSV row: $row - ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Parsed ${books.size} valid books from CSV")

            if (books.isNotEmpty()) {
                replaceAll(books)
                Log.d(TAG, "Asset import completed successfully")
            } else {
                Log.w(TAG, "No valid books found in CSV")
            }

            inputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error importing from assets: ${e.message}", e)
        }
    }

    // Helper method for full sync (bidirectional)
    fun fullSync() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting full sync")
                val result = repository.fullSync()
                when (result) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "Full sync successful")
                    }
                    is SyncResult.Error -> {
                        Log.e(TAG, "Full sync failed: ${result.message}")
                    }
                    is SyncResult.Partial -> {
                        Log.w(TAG, "Full sync partial: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Full sync error: ${e.message}", e)
            }
        }
    }
}