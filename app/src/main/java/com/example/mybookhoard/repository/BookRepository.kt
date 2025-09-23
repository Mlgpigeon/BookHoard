package com.example.mybookhoard.repository

import android.content.Context
import android.util.Log
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import com.example.mybookhoard.utils.FuzzySearchUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class BookRepository(private val context: Context) {

    companion object {
        private const val TAG = "BookRepository"
        private const val SESSION_VERIFICATION_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val apiService = ApiService(context)
    private val localDao = AppDb.get(context).bookDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Offline)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState

    init {
        // Initialize auth state by checking saved session
        initializeAuthState()
    }

    private fun initializeAuthState() {
        repositoryScope.launch {
            Log.d(TAG, "Initializing auth state...")

            if (!apiService.hasValidSession()) {
                Log.d(TAG, "No valid session found")
                _authState.value = AuthState.NotAuthenticated
                _connectionState.value = ConnectionState.Offline
                return@launch
            }

            val savedUser = apiService.getSavedUser()
            val token = apiService.getAuthToken()

            if (savedUser == null || token == null) {
                Log.w(TAG, "Incomplete session data, clearing")
                apiService.clearUserSession()
                _authState.value = AuthState.NotAuthenticated
                _connectionState.value = ConnectionState.Offline
                return@launch
            }

            Log.d(TAG, "Found saved session for user: ${savedUser.username}")

            // Set authenticated state immediately with saved data
            _authState.value = AuthState.Authenticated(savedUser, token)
            _connectionState.value = ConnectionState.Offline

            // Check if we need to verify the session (if it's been a while)
            val lastSync = apiService.getLastSyncTime()
            val shouldVerify = System.currentTimeMillis() - lastSync > SESSION_VERIFICATION_INTERVAL

            if (shouldVerify) {
                Log.d(TAG, "Session verification needed, checking with server...")
                verifyAndRefreshSession(savedUser, token)
            } else {
                Log.d(TAG, "Session recently verified, using cached data")
                // Still try to sync in background but don't change auth state
                backgroundSync()
            }
        }
    }

    private suspend fun verifyAndRefreshSession(savedUser: User, token: String) {
        try {
            when (val result = apiService.getProfile()) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Session verified successfully")
                    _authState.value = AuthState.Authenticated(result.data, token)
                    _connectionState.value = ConnectionState.Online
                    // Sync data after successful verification
                    syncFromServer()
                }
                is ApiResult.Error -> {
                    if (result.message.contains("401") || result.message.contains("unauthorized", ignoreCase = true)) {
                        Log.w(TAG, "Session expired, need to re-authenticate")
                        apiService.clearUserSession()
                        _authState.value = AuthState.NotAuthenticated
                        _connectionState.value = ConnectionState.Offline
                    } else {
                        Log.w(TAG, "Session verification failed due to network: ${result.message}")
                        // Keep user authenticated but mark as offline/error
                        _authState.value = AuthState.Authenticated(savedUser, token)
                        _connectionState.value = ConnectionState.Error("Unable to verify session: ${result.message}")
                        // Retry verification later
                        scheduleSessionRetry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying session: ${e.message}")
            // Keep user authenticated but mark as offline
            _authState.value = AuthState.Authenticated(savedUser, token)
            _connectionState.value = ConnectionState.Error("Network error: ${e.message}")
            scheduleSessionRetry()
        }
    }

    private fun scheduleSessionRetry() {
        repositoryScope.launch {
            delay(30_000) // Retry after 30 seconds
            val currentAuth = _authState.value
            if (currentAuth is AuthState.Authenticated && _connectionState.value is ConnectionState.Error) {
                Log.d(TAG, "Retrying session verification...")
                verifyAndRefreshSession(currentAuth.user, currentAuth.token)
            }
        }
    }

    private suspend fun backgroundSync() {
        try {
            _connectionState.value = ConnectionState.Syncing
            val result = apiService.getBooks()
            when (result) {
                is ApiResult.Success -> {
                    val serverBooks = result.data.map { it.toLocalBook() }
                    localDao.upsertAll(serverBooks)
                    _connectionState.value = ConnectionState.Online
                    Log.d(TAG, "Background sync successful: ${serverBooks.size} books")
                }
                is ApiResult.Error -> {
                    _connectionState.value = ConnectionState.Error(result.message)
                    Log.w(TAG, "Background sync failed: ${result.message}")
                }
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Network error")
            Log.e(TAG, "Background sync error: ${e.message}")
        }
    }

    // Authentication methods
    suspend fun register(username: String, email: String, password: String): AuthResult {
        Log.d(TAG, "Registering user: $username")
        _authState.value = AuthState.Authenticating

        return when (val result = apiService.register(username, email, password)) {
            is AuthResult.Success -> {
                Log.d(TAG, "Registration successful")
                _authState.value = AuthState.Authenticated(result.user, result.token)
                _connectionState.value = ConnectionState.Online
                // Sync data after successful registration
                syncFromServer()
                result
            }
            is AuthResult.Error -> {
                Log.e(TAG, "Registration failed: ${result.message}")
                _authState.value = AuthState.Error(result.message)
                _connectionState.value = ConnectionState.Error("Registration failed")
                result
            }
        }
    }

    suspend fun login(identifier: String, password: String): AuthResult {
        Log.d(TAG, "Logging in user: $identifier")
        _authState.value = AuthState.Authenticating

        return when (val result = apiService.login(identifier, password)) {
            is AuthResult.Success -> {
                Log.d(TAG, "Login successful")
                _authState.value = AuthState.Authenticated(result.user, result.token)
                _connectionState.value = ConnectionState.Online
                // Sync data after successful login
                syncFromServer()
                result
            }
            is AuthResult.Error -> {
                Log.e(TAG, "Login failed: ${result.message}")
                _authState.value = AuthState.Error(result.message)
                _connectionState.value = ConnectionState.Error("Login failed")
                result
            }
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        Log.d(TAG, "Logging out user")
        val result = apiService.logout()
        _authState.value = AuthState.NotAuthenticated
        _connectionState.value = ConnectionState.Offline

        // Clear local data on logout
        try {
            localDao.clear()
            Log.d(TAG, "Local data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local data: ${e.message}")
        }

        return result
    }

    suspend fun getProfile(): ApiResult<User> {
        return apiService.getProfile()
    }

    // Session management
    fun hasValidSession(): Boolean {
        return apiService.hasValidSession() && _authState.value is AuthState.Authenticated
    }

    fun refreshSession() {
        if (hasValidSession()) {
            val currentAuth = _authState.value
            if (currentAuth is AuthState.Authenticated) {
                repositoryScope.launch {
                    verifyAndRefreshSession(currentAuth.user, currentAuth.token)
                }
            }
        }
    }

    // Book data methods
    fun getAllBooks(): Flow<List<Book>> {
        return if (isAuthenticated()) {
            // If authenticated, use local data and sync in background
            flow {
                // Emit local data first
                val localBooks = localDao.all().first()
                emit(localBooks)

                // Try to sync in background if not already syncing and online
                if (_connectionState.value !is ConnectionState.Syncing && _connectionState.value is ConnectionState.Online) {
                    try {
                        backgroundSync()
                        // Emit updated data after sync
                        val updatedBooks = localDao.all().first()
                        emit(updatedBooks)
                    } catch (e: Exception) {
                        Log.e(TAG, "Background sync in getAllBooks failed: ${e.message}")
                    }
                }
            }.flowOn(Dispatchers.IO)
        } else {
            // If not authenticated, use local data only
            localDao.all()
        }
    }

    suspend fun addBook(book: Book): Boolean {
        return if (isAuthenticated()) {
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
        return if (isAuthenticated() && book.id > 0) {
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
                    localDao.upsertAll(listOf(book))
                    Log.w(TAG, "Book updated locally only: ${book.title} - ${result.message}")
                    false
                }
            }
        } else {
            // Update locally only
            localDao.upsertAll(listOf(book))
            Log.d(TAG, "Book updated locally: ${book.title}")
            true
        }
    }

    suspend fun deleteBook(book: Book): Boolean {
        return if (isAuthenticated() && book.id > 0) {
            // Delete from server first
            when (val result = apiService.deleteBook(book.id)) {
                is ApiResult.Success -> {
                    // Delete from local database
                    localDao.delete(book)
                    Log.d(TAG, "Book deleted from server and local DB: ${book.title}")
                    true
                }
                is ApiResult.Error -> {
                    // Mark for deletion locally (could implement soft delete)
                    localDao.delete(book)
                    Log.w(TAG, "Book deleted locally only: ${book.title} - ${result.message}")
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

    fun getBookById(id: Long): Flow<Book?> {
        return localDao.getBookById(id)
    }

    fun searchBooks(query: String): Flow<List<Book>> {
        return if (query.isBlank()) {
            getAllBooks()
        } else {
            localDao.all().map { books ->
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = 0.25)
            }
        }
    }

    fun getWishlistBooks(): Flow<List<Book>> {
        return localDao.all().map { books ->
            books.filter {
                it.wishlist == WishlistStatus.WISH || it.wishlist == WishlistStatus.ON_THE_WAY
            }
        }
    }

    fun searchWishlistBooks(query: String): Flow<List<Book>> {
        return getWishlistBooks().map { books ->
            if (query.isBlank()) {
                books
            } else {
                FuzzySearchUtils.searchBooksSimple(books, query, threshold = 0.25)
            }
        }
    }

    fun getUniqueAuthors(): Flow<List<String>> {
        return localDao.getUniqueAuthors()
    }

    fun getUniqueSagas(): Flow<List<String>> {
        return localDao.getUniqueSagas()
    }

    // Sync operations
    suspend fun syncFromServer(): SyncResult {
        if (!isAuthenticated()) {
            return SyncResult.Error("Not authenticated")
        }

        return try {
            Log.d(TAG, "Starting sync from server...")
            _connectionState.value = ConnectionState.Syncing

            when (val result = apiService.getBooks()) {
                is ApiResult.Success -> {
                    val serverBooks = result.data.map { it.toLocalBook() }
                    localDao.clear()
                    localDao.upsertAll(serverBooks)
                    _connectionState.value = ConnectionState.Online
                    Log.d(TAG, "Sync from server successful: ${serverBooks.size} books")
                    SyncResult.Success
                }
                is ApiResult.Error -> {
                    _connectionState.value = ConnectionState.Error(result.message)
                    Log.e(TAG, "Sync from server failed: ${result.message}")
                    SyncResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Sync failed")
            Log.e(TAG, "Sync from server error: ${e.message}")
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun syncToServer(): SyncResult {
        if (!isAuthenticated()) {
            return SyncResult.Error("Not authenticated")
        }

        return try {
            Log.d(TAG, "Starting sync to server...")
            _connectionState.value = ConnectionState.Syncing

            val localBooks = localDao.all().first()
            var successful = 0
            var failed = 0

            localBooks.forEach { book ->
                val apiBook = ApiBook.fromLocalBook(book)
                val result = if (book.id > 0) {
                    apiService.updateBook(book.id, apiBook)
                } else {
                    apiService.createBook(apiBook)
                }

                when (result) {
                    is ApiResult.Success -> {
                        successful++
                        // Update local book with server data
                        val serverBook = result.data.toLocalBook()
                        localDao.upsertAll(listOf(serverBook))
                    }
                    is ApiResult.Error -> {
                        failed++
                        Log.w(TAG, "Failed to sync book '${book.title}': ${result.message}")
                    }
                }
            }

            _connectionState.value = ConnectionState.Online
            Log.d(TAG, "Sync to server completed: $successful successful, $failed failed")

            if (failed == 0) {
                SyncResult.Success
            } else {
                SyncResult.Partial("Some books failed to sync", successful, failed)
            }

        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Sync failed")
            Log.e(TAG, "Sync to server error: ${e.message}")
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun replaceAllBooks(books: List<Book>) {
        localDao.clear()
        localDao.upsertAll(books)
        Log.d(TAG, "Replaced all books: ${books.size} books")
    }

    // Clear error states
    fun clearAuthError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    fun clearConnectionError() {
        if (_connectionState.value is ConnectionState.Error) {
            val currentAuth = _authState.value
            if (currentAuth is AuthState.Authenticated) {
                _connectionState.value = ConnectionState.Offline
                // Try to reconnect
                refreshSession()
            } else {
                _connectionState.value = ConnectionState.Offline
            }
        }
    }

    private fun isAuthenticated(): Boolean {
        return _authState.value is AuthState.Authenticated
    }
}