package com.example.mybookhoard.repositories

import android.content.Context
import android.util.Log
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles synchronization between local database and server
 */
class BookSyncService(
    private val context: Context,
    private val apiService: ApiService,
    private val localDao: BookDao,
    private val authStateManager: AuthStateManager,
    private val connectionStateManager: ConnectionStateManager
) {
    companion object {
        private const val TAG = "BookSyncService"
    }

    private val syncScope = CoroutineScope(Dispatchers.IO)

    // Main sync operations
    suspend fun syncFromServer(): SyncResult {
        if (!authStateManager.isAuthenticated()) {
            return SyncResult.Error("Not authenticated")
        }

        return try {
            Log.d(TAG, "Starting sync from server...")
            connectionStateManager.setSyncing()

            when (val result = apiService.getBooks()) {
                is ApiResult.Success -> {
                    val serverBooks = result.data.map { it.toLocalBook() }
                    localDao.clear()
                    localDao.upsertAll(serverBooks)
                    connectionStateManager.setOnline()
                    Log.d(TAG, "Sync from server successful: ${serverBooks.size} books")
                    SyncResult.Success
                }
                is ApiResult.Error -> {
                    connectionStateManager.setError(result.message)
                    Log.e(TAG, "Sync from server failed: ${result.message}")
                    SyncResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Sync failed"
            connectionStateManager.setError(errorMsg)
            Log.e(TAG, "Sync from server error: $errorMsg")
            SyncResult.Error(errorMsg)
        }
    }

    suspend fun syncToServer(): SyncResult {
        if (!authStateManager.isAuthenticated()) {
            return SyncResult.Error("Not authenticated")
        }

        return try {
            Log.d(TAG, "Starting sync to server...")
            connectionStateManager.setSyncing()

            // Get all local books
            val localBooks = mutableListOf<Book>()
            localDao.all().collect { books ->
                localBooks.clear()
                localBooks.addAll(books)
            }

            if (localBooks.isEmpty()) {
                connectionStateManager.setOnline()
                return SyncResult.Success
            }

            var successful = 0
            var failed = 0
            val errors = mutableListOf<String>()

            // Sync each book to server
            for (book in localBooks) {
                try {
                    val apiBook = ApiBook.fromLocalBook(book)
                    val result = if (book.id > 0) {
                        apiService.updateBook(book.id, apiBook)
                    } else {
                        apiService.createBook(apiBook)
                    }

                    when (result) {
                        is ApiResult.Success -> {
                            // Update local book with server response
                            val serverBook = result.data.toLocalBook()
                            localDao.upsert(serverBook)
                            successful++
                        }
                        is ApiResult.Error -> {
                            errors.add("${book.title}: ${result.message}")
                            failed++
                        }
                    }
                } catch (e: Exception) {
                    errors.add("${book.title}: ${e.message}")
                    failed++
                }
            }

            connectionStateManager.setOnline()
            Log.d(TAG, "Sync to server completed: $successful successful, $failed failed")

            if (failed == 0) {
                SyncResult.Success
            } else {
                SyncResult.Partial(
                    message = "Partial sync: $successful successful, $failed failed",
                    successful = successful,
                    failed = failed
                )
            }

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Sync to server failed"
            connectionStateManager.setError(errorMsg)
            Log.e(TAG, "Sync to server error: $errorMsg")
            SyncResult.Error(errorMsg)
        }
    }

    suspend fun fullSync(): SyncResult {
        if (!authStateManager.isAuthenticated()) {
            return SyncResult.Error("Not authenticated")
        }

        Log.d(TAG, "Starting full sync...")

        // First sync from server to get latest data
        val syncFromResult = syncFromServer()
        if (syncFromResult is SyncResult.Error) {
            return syncFromResult
        }

        // Then sync any local changes back to server
        return syncToServer()
    }

    // Background sync operations
    fun startBackgroundSync() {
        if (!authStateManager.isAuthenticated() || !connectionStateManager.isOnline()) {
            return
        }

        syncScope.launch {
            try {
                Log.d(TAG, "Starting background sync...")
                syncFromServer()
            } catch (e: Exception) {
                Log.w(TAG, "Background sync failed: ${e.message}")
            }
        }
    }

    // Book-specific sync operations
    suspend fun syncSingleBook(book: Book): Boolean {
        if (!authStateManager.isAuthenticated()) {
            Log.w(TAG, "Cannot sync book - not authenticated")
            return false
        }

        return try {
            val apiBook = ApiBook.fromLocalBook(book)
            val result = if (book.id > 0) {
                apiService.updateBook(book.id, apiBook)
            } else {
                apiService.createBook(apiBook)
            }

            when (result) {
                is ApiResult.Success -> {
                    // Update local database with server response
                    val serverBook = result.data.toLocalBook()
                    localDao.upsert(serverBook)
                    Log.d(TAG, "Single book sync successful: ${serverBook.title}")
                    true
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Single book sync failed: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Single book sync error: ${e.message}")
            false
        }
    }

    // Check if local changes need syncing
    suspend fun hasLocalChanges(): Boolean {
        // This would require a more sophisticated change tracking system
        // For now, we'll assume there might be changes if we have books and are authenticated
        val count = localDao.count()
        return count > 0 && authStateManager.isAuthenticated()
    }

    // Get last sync time (would need to be implemented with SharedPreferences)
    suspend fun getLastSyncTime(): Long {
        // TODO: Implement with SharedPreferences
        return 0L
    }

    // Set last sync time
    suspend fun setLastSyncTime(timestamp: Long) {
        // TODO: Implement with SharedPreferences
    }
}