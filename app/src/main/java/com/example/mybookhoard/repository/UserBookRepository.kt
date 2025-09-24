// File: app/src/main/java/com/example/mybookhoard/repository/UserBookRepository.kt

package com.example.mybookhoard.repository

import android.content.Context
import android.util.Log
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import com.example.mybookhoard.repository.AuthStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * UserBook repository that handles all UserBook-specific operations
 * Completely separate from BookRepository logic
 */
class UserBookRepository(private val context: Context) {

    companion object {
        private const val TAG = "UserBookRepository"
    }

    // Core services (reusing shared components from BookRepository pattern)
    private val apiService = ApiService(context)
    private val localUserBookDao = AppDb.get(context).userBookDao()
    private val localBookDao = AppDb.get(context).bookDao()

    // Shared managers (same as BookRepository)
    private val authStateManager = AuthStateManager(context, apiService)
    private val connectionStateManager = ConnectionStateManager(context, apiService, authStateManager)

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Public state flows (following BookRepository pattern)
    val connectionState: StateFlow<ConnectionState> = connectionStateManager.connectionState
    val authState: StateFlow<AuthState> = authStateManager.authState

    // UserBook CRUD operations (following BookRepository pattern)
    fun getAllUserBooks(): Flow<List<UserBook>> {
        return if (authStateManager.isAuthenticated()) {
            val currentUser = authStateManager.getCurrentUser()
            if (currentUser != null) {
                localUserBookDao.getUserBooks(currentUser.id).flowOn(Dispatchers.IO).onStart {
                    // Start background sync (following BookRepository pattern)
                    repositoryScope.launch {
                        try {
                            startBackgroundSync()
                        } catch (e: Exception) {
                            Log.w(TAG, "Background sync in getAllUserBooks failed: ${e.message}")
                        }
                    }
                }.flowOn(Dispatchers.IO)
            } else {
                flowOf(emptyList())
            }
        } else {
            // If not authenticated, return empty (UserBooks are user-specific)
            flowOf(emptyList())
        }
    }

    suspend fun addGoogleBookAsUserBook(
        title: String,
        author: String? = null,
        saga: String? = null,
        description: String? = null,
        wishlistStatus: UserBookWishlistStatus
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (authStateManager.isAuthenticated()) {
            // Add to server first (following BookRepository pattern)
            when (val result = apiService.addGoogleBookAsUserBook(title, author, saga, description, wishlistStatus)) {
                is ApiResult.Success -> {
                    val apiUserBook = result.data

                    // Ensure the Book exists locally first
                    val book = Book(
                        id = apiUserBook.bookId,
                        title = apiUserBook.title,
                        author = apiUserBook.author,
                        saga = apiUserBook.saga,
                        description = apiUserBook.description
                    )
                    localBookDao.upsert(book)

                    // Add UserBook to local database with server data
                    val userBook = apiUserBook.toLocalUserBook()
                    localUserBookDao.insert(userBook)

                    Log.d(TAG, "UserBook added to server and local DB: $title")
                    true
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "UserBook could not be added to server: ${result.message}")
                    false
                }
            }
        } else {
            Log.e(TAG, "Cannot add UserBook: Authentication required")
            false
        }
    }

    suspend fun updateUserBook(userBook: UserBook): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (authStateManager.isAuthenticated()) {
            // Update server first (following BookRepository pattern)
            val updateData = mapOf(
                "status" to userBook.readingStatus.name,
                "wishlist" to userBook.wishlistStatus?.name,
                "personal_rating" to userBook.personalRating,
                "review" to userBook.review,
                "reading_progress" to userBook.readingProgress,
                "favorite" to userBook.favorite
            )

            when (val result = apiService.updateUserBook(userBook.id, updateData)) {
                is ApiResult.Success -> {
                    // Update local database with server response
                    val updatedUserBook = result.data.toLocalUserBook()
                    localUserBookDao.update(updatedUserBook)
                    Log.d(TAG, "UserBook updated on server and locally: ${userBook.id}")
                    true
                }
                is ApiResult.Error -> {
                    // Save locally for later sync (following BookRepository pattern)
                    localUserBookDao.update(userBook.copy(updatedAt = java.util.Date()))
                    Log.w(TAG, "UserBook updated locally only: ${userBook.id} - ${result.message}")
                    false
                }
            }
        } else {
            // Update locally (offline capability)
            localUserBookDao.update(userBook.copy(updatedAt = java.util.Date()))
            Log.d(TAG, "UserBook updated locally: ${userBook.id}")
            true
        }
    }

    suspend fun deleteUserBook(userBook: UserBook): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (authStateManager.isAuthenticated()) {
            // Delete from server first (following BookRepository pattern)
            when (val result = apiService.deleteUserBook(userBook.id)) {
                is ApiResult.Success -> {
                    // Delete from local database
                    localUserBookDao.delete(userBook)
                    Log.d(TAG, "UserBook deleted from server and locally: ${userBook.id}")
                    true
                }
                is ApiResult.Error -> {
                    // Delete locally anyway (following BookRepository pattern for user-initiated deletes)
                    localUserBookDao.delete(userBook)
                    Log.w(TAG, "UserBook deleted locally only: ${userBook.id} - ${result.message}")
                    false
                }
            }
        } else {
            // Delete locally (offline capability)
            localUserBookDao.delete(userBook)
            Log.d(TAG, "UserBook deleted locally: ${userBook.id}")
            true
        }
    }

    // Search operations (direct implementation for UserBooks)
    fun getUserBooks(userId: Long): Flow<List<UserBook>> =
        localUserBookDao.getUserBooks(userId)

    fun getUserBookById(id: Long, userId: Long): Flow<UserBook?> =
        localUserBookDao.getUserBookById(id, userId)

    fun getUserBooksByStatus(userId: Long, status: UserBookReadingStatus): Flow<List<UserBook>> =
        localUserBookDao.getUserBooksByStatus(userId, status)

    fun getUserBooksByWishlistStatus(userId: Long, status: UserBookWishlistStatus): Flow<List<UserBook>> =
        localUserBookDao.getUserBooksByWishlistStatus(userId, status)

    fun searchUserBooks(userId: Long, query: String): Flow<List<UserBook>> =
        localUserBookDao.searchUserBooks(userId, "%$query%")

    fun getFavoriteUserBooks(userId: Long): Flow<List<UserBook>> =
        localUserBookDao.getFavoriteBooks(userId)

    fun getCurrentlyReading(userId: Long): Flow<List<UserBook>> =
        localUserBookDao.getCurrentlyReading(userId)

    fun getRatedBooks(userId: Long): Flow<List<UserBook>> =
        localUserBookDao.getRatedBooks(userId)

    // Statistics operations (direct implementation)
    suspend fun getTotalUserBooksCount(userId: Long): Int =
        withContext(Dispatchers.IO) {
            localUserBookDao.getTotalBooksCount(userId)
        }

    suspend fun getCountByStatus(userId: Long, status: UserBookReadingStatus): Int =
        withContext(Dispatchers.IO) {
            localUserBookDao.getCountByStatus(userId, status)
        }

    suspend fun getCountByWishlistStatus(userId: Long, status: UserBookWishlistStatus): Int =
        withContext(Dispatchers.IO) {
            localUserBookDao.getCountByWishlistStatus(userId, status)
        }

    suspend fun getAverageRating(userId: Long): Float? =
        withContext(Dispatchers.IO) {
            localUserBookDao.getAverageRating(userId)
        }

    // Sync operations (UserBook-specific implementation)
    suspend fun syncFromServer(): SyncResult = withContext(Dispatchers.IO) {
        if (!authStateManager.isAuthenticated()) {
            return@withContext SyncResult.Error("Authentication required")
        }

        try {
            val currentUser = authStateManager.getCurrentUser()
                ?: return@withContext SyncResult.Error("User not found")

            when (val result = apiService.getUserBooks()) {
                is ApiResult.Success -> {
                    val serverUserBooks = result.data.map { it.toLocalUserBook() }

                    // Replace all local UserBooks with server data
                    localUserBookDao.deleteAllUserBooks(currentUser.id)
                    localUserBookDao.insertAll(serverUserBooks)

                    Log.d(TAG, "Synced ${serverUserBooks.size} UserBooks from server")
                    SyncResult.Success
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to sync UserBooks from server: ${result.message}")
                    SyncResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync from server failed: ${e.message}", e)
            SyncResult.Error("Sync failed: ${e.message}")
        }
    }

    suspend fun syncToServer(): SyncResult = withContext(Dispatchers.IO) {
        if (!authStateManager.isAuthenticated()) {
            return@withContext SyncResult.Error("Authentication required")
        }

        try {
            val currentUser = authStateManager.getCurrentUser()
                ?: return@withContext SyncResult.Error("User not found")

            // Get all local UserBooks that need syncing
            val localUserBooks = localUserBookDao.getUserBooks(currentUser.id).first()

            var successful = 0
            var failed = 0

            for (userBook in localUserBooks) {
                try {
                    // This is a simplified sync - in a real implementation you'd track
                    // what needs to be synced vs what's already on server
                    val updateData = mapOf(
                        "status" to userBook.readingStatus.name,
                        "wishlist" to userBook.wishlistStatus?.name,
                        "personal_rating" to userBook.personalRating,
                        "review" to userBook.review,
                        "reading_progress" to userBook.readingProgress,
                        "favorite" to userBook.favorite
                    )

                    when (apiService.updateUserBook(userBook.id, updateData)) {
                        is ApiResult.Success -> successful++
                        is ApiResult.Error -> failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(TAG, "Failed to sync UserBook ${userBook.id}: ${e.message}")
                }
            }

            if (failed == 0) {
                Log.d(TAG, "Successfully synced $successful UserBooks to server")
                SyncResult.Success
            } else {
                SyncResult.Partial("Partial sync completed", successful, failed)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync to server failed: ${e.message}", e)
            SyncResult.Error("Sync failed: ${e.message}")
        }
    }

    suspend fun fullSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Sync to server first, then from server
            val uploadResult = syncToServer()
            val downloadResult = syncFromServer()

            when {
                uploadResult is SyncResult.Success && downloadResult is SyncResult.Success -> {
                    Log.d(TAG, "Full UserBook sync completed successfully")
                    SyncResult.Success
                }
                else -> {
                    Log.w(TAG, "Full UserBook sync completed with issues")
                    SyncResult.Partial("Full sync completed with some issues", 0, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Full UserBook sync failed: ${e.message}", e)
            SyncResult.Error("Full sync failed: ${e.message}")
        }
    }

    // Background sync (following BookRepository pattern)
    private suspend fun startBackgroundSync() {
        try {
            if (authStateManager.isAuthenticated()) {
                Log.d(TAG, "Starting background UserBook sync")
                fullSync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Background UserBook sync failed: ${e.message}")
        }
    }

    // Helper methods (following BookRepository pattern)
    fun isAuthenticated(): Boolean = authStateManager.isAuthenticated()

    fun getCurrentUser(): User? = authStateManager.getCurrentUser()

    suspend fun testConnection(): Boolean = connectionStateManager.testConnection()
}