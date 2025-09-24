package com.example.mybookhoard.repository

import android.content.Context
import android.util.Log
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Main repository coordinator that delegates to specialized services
 */
class BookRepository(private val context: Context) {

    companion object {
        private const val TAG = "BookRepository"
    }

    // Core services
    private val apiService = ApiService(context)
    private val localDao = AppDb.get(context).bookDao()

    // Specialized service managers
    private val authStateManager = AuthStateManager(context, apiService)
    private val connectionStateManager = ConnectionStateManager(context, apiService, authStateManager)
    private val searchService = BookSearchService(context, localDao)
    private val syncService = BookSyncService(context, apiService, localDao, authStateManager, connectionStateManager)

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Public state flows
    val connectionState: StateFlow<ConnectionState> = connectionStateManager.connectionState
    val authState: StateFlow<AuthState> = authStateManager.authState

    // Authentication operations
    suspend fun login(username: String, password: String): AuthResult {
        return authStateManager.login(username, password)
    }

    suspend fun register(username: String, email: String, password: String): AuthResult {
        return authStateManager.register(username, email, password)
    }

    fun logout() {
        authStateManager.logout()
    }

    fun isAuthenticated(): Boolean {
        return authStateManager.isAuthenticated()
    }

    fun getCurrentUser(): User? {
        return authStateManager.getCurrentUser()
    }

    // Connection operations
    suspend fun testConnection(): Boolean {
        return connectionStateManager.testConnection()
    }

    // Book CRUD operations
    fun getAllBooks(): Flow<List<Book>> {
        return if (authStateManager.isAuthenticated()) {
            searchService.searchBooks("").flowOn(Dispatchers.IO).onStart {
                // Start background sync
                repositoryScope.launch {
                    try {
                        syncService.startBackgroundSync()
                    } catch (e: Exception) {
                        Log.w(TAG, "Background sync in getAllBooks failed: ${e.message}")
                    }
                }
            }.flowOn(Dispatchers.IO)
        } else {
            // If not authenticated, use local data only
            localDao.all()
        }
    }

    suspend fun addBook(book: Book): Boolean {
        return if (authStateManager.isAuthenticated()) {
            // Add to server first
            val apiBook = ApiBook.fromLocalBook(book)
            when (val result = apiService.createBook(apiBook)) {
                is ApiResult.Success -> {
                    // Add to local database with server ID
                    val serverBook = result.data.toLocalBook()
                    localDao.upsertAll(listOf(serverBook))
                    Log.d(TAG, "Book added to server and local DB: ${serverBook.title}")
                    true
                }
                is ApiResult.Error -> {
                    // Save locally for later sync
                    localDao.upsertAll(listOf(book))
                    Log.w(TAG, "Book saved locally only: ${book.title} - ${result.message}")
                    false
                }
            }
        } else {
            // Save locally only
            localDao.upsertAll(listOf(book))
            Log.d(TAG, "Book saved locally (offline): ${book.title}")
            true
        }
    }

    suspend fun updateBook(book: Book): Boolean {
        return if (authStateManager.isAuthenticated() && book.id > 0) {
            // Update on server first
            val apiBook = ApiBook.fromLocalBook(book)
            when (val result = apiService.updateBook(book.id, apiBook)) {
                is ApiResult.Success -> {
                    // Update local database
                    val serverBook = result.data.toLocalBook()
                    localDao.upsertAll(listOf(serverBook))
                    Log.d(TAG, "Book updated on server and local DB: ${serverBook.title}")
                    true
                }
                is ApiResult.Error -> {
                    // Update locally for later sync
                    localDao.update(book)
                    Log.w(TAG, "Book updated locally only: ${book.title} - ${result.message}")
                    false
                }
            }
        } else {
            // Update locally only
            localDao.update(book)
            Log.d(TAG, "Book updated locally: ${book.title}")
            true
        }
    }

    suspend fun deleteBook(book: Book): Boolean {
        return if (authStateManager.isAuthenticated() && book.id > 0) {
            when (val result = apiService.deleteBook(book.id)) {
                is ApiResult.Success -> {
                    localDao.delete(book)
                    Log.d(TAG, "Book deleted from server and local DB: ${book.title}")
                    true
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to delete book from server: ${result.message}")
                    false
                }
            }
        } else {
            // Delete locally only
            localDao.delete(book)
            Log.d(TAG, "Book deleted locally: ${book.title}")
            true
        }
    }

    // Search operations (delegated to SearchService)
    fun searchBooks(query: String): Flow<List<Book>> = searchService.searchBooks(query)

    fun getBooksByStatus(status: ReadingStatus): Flow<List<Book>> =
        searchService.getBooksByStatus(status)

    fun searchBooksByStatus(status: ReadingStatus, query: String): Flow<List<Book>> =
        searchService.searchBooksByStatus(status, query)

    fun getBooksByAuthor(author: String): Flow<List<Book>> =
        searchService.getBooksByAuthor(author)

    fun getBooksBySaga(saga: String): Flow<List<Book>> =
        searchService.getBooksBySaga(saga)

    fun getWishlistBooks(): Flow<List<Book>> = searchService.getWishlistBooks()

    fun searchWishlistBooks(query: String): Flow<List<Book>> =
        searchService.searchWishlistBooks(query)

    fun getUniqueAuthors(): Flow<List<String>> = searchService.getUniqueAuthors()

    fun getUniqueSagas(): Flow<List<String>> = searchService.getUniqueSagas()

    fun getBookById(id: Long): Flow<Book?> = searchService.getBookById(id)

    // Sync operations (delegated to SyncService)
    suspend fun syncFromServer(): SyncResult = syncService.syncFromServer()

    suspend fun syncToServer(): SyncResult = syncService.syncToServer()

    suspend fun fullSync(): SyncResult = syncService.fullSync()

    // Network search operations
    suspend fun searchBooksWithGoogleBooks(query: String, limit: Int = 20): ApiResult<CombinedSearchResponse> {
        return if (authStateManager.isAuthenticated()) {
            apiService.searchBooksWithGoogleBooks(query, limit)
        } else {
            ApiResult.Error("Authentication required for Google Books search")
        }
    }

    suspend fun addGoogleBook(
        title: String,
        author: String? = null,
        saga: String? = null,
        description: String? = null,
        wishlistStatus: WishlistStatus
    ): ApiResult<Book> {
        return if (authStateManager.isAuthenticated()) {
            apiService.addGoogleBook(title, author, saga, description, wishlistStatus)
        } else {
            ApiResult.Error("Authentication required to add Google Books")
        }
    }

    // Statistics operations
    suspend fun getTotalBooksCount(): Int = searchService.getTotalBooksCount()

    suspend fun getCountByStatus(status: ReadingStatus): Int =
        searchService.getCountByStatus(status)

    suspend fun getCountByWishlistStatus(wishlistStatus: WishlistStatus): Int =
        searchService.getCountByWishlistStatus(wishlistStatus)

    // Advanced search with filters
    fun getBooksWithFilters(
        status: ReadingStatus? = null,
        author: String? = null,
        saga: String? = null,
        wishlistStatus: WishlistStatus? = null,
        query: String = ""
    ): Flow<List<Book>> {
        return searchService.getBooksWithMultipleFilters(status, author, saga, wishlistStatus, query)
    }

    // ADDED: Missing method for bulk operations
    suspend fun replaceAllBooks(books: List<Book>) {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Replacing all books with ${books.size} books")

                // Clear existing books and add new ones
                localDao.clear()
                localDao.upsertAll(books)

                Log.d(TAG, "All books replaced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error replacing all books: ${e.message}", e)
                throw e
            }
        }
    }
}