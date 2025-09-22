package com.example.mybookhoard.repository

import android.content.Context
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import com.example.mybookhoard.utils.FuzzySearchUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class BookRepository(private val context: Context) {

    private val apiService = ApiService(context)
    private val localDao = AppDb.get(context).bookDao()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Offline)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState

    init {
        // Check if user is already authenticated
        if (apiService.getAuthToken() != null) {
            _authState.value = AuthState.Authenticating
            // We could verify token here, but for now assume it's valid
        }
    }

    // Authentication methods
    suspend fun register(username: String, email: String, password: String): AuthResult {
        _authState.value = AuthState.Authenticating

        return when (val result = apiService.register(username, email, password)) {
            is AuthResult.Success -> {
                _authState.value = AuthState.Authenticated(result.user, result.token)
                _connectionState.value = ConnectionState.Online
                result
            }
            is AuthResult.Error -> {
                _authState.value = AuthState.Error(result.message)
                result
            }
        }
    }

    suspend fun login(identifier: String, password: String): AuthResult {
        _authState.value = AuthState.Authenticating

        return when (val result = apiService.login(identifier, password)) {
            is AuthResult.Success -> {
                _authState.value = AuthState.Authenticated(result.user, result.token)
                _connectionState.value = ConnectionState.Online
                // Sync data after successful login
                syncFromServer()
                result
            }
            is AuthResult.Error -> {
                _authState.value = AuthState.Error(result.message)
                result
            }
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        val result = apiService.logout()
        _authState.value = AuthState.NotAuthenticated
        _connectionState.value = ConnectionState.Offline

        // Clear local data on logout
        localDao.clear()

        return result
    }

    suspend fun getProfile(): ApiResult<User> {
        return apiService.getProfile()
    }

    // Book data methods
    fun getAllBooks(): Flow<List<Book>> {
        return if (isAuthenticated()) {
            // If authenticated, try to keep data synced
            flow {
                emit(localDao.all().first())

                // Try to sync in background
                try {
                    _connectionState.value = ConnectionState.Syncing
                    val apiResult = apiService.getBooks()
                    if (apiResult.isSuccess()) {
                        val serverBooks = apiResult.getOrNull()!!.map { it.toLocalBook() }
                        localDao.upsertAll(serverBooks)
                        _connectionState.value = ConnectionState.Online
                        emit(serverBooks)
                    } else {
                        _connectionState.value = ConnectionState.Error(apiResult.errorMessage() ?: "Sync failed")
                    }
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.Error(e.message ?: "Network error")
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
                    true
                }
                is ApiResult.Error -> {
                    // Save locally for later sync
                    localDao.upsertAll(listOf(book))
                    false
                }
            }
        } else {
            // Save locally only
            localDao.upsertAll(listOf(book))
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
                    true
                }
                is ApiResult.Error -> {
                    // Update locally for later sync
                    localDao.upsertAll(listOf(book))
                    false
                }
            }
        } else {
            // Update locally only
            localDao.upsertAll(listOf(book))
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
                    true
                }
                is ApiResult.Error -> {
                    // Mark for deletion locally (could implement soft delete)
                    localDao.delete(book)
                    false
                }
            }
        } else {
            // Delete locally only
            localDao.delete(book)
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
            _connectionState.value = ConnectionState.Syncing

            when (val result = apiService.getBooks()) {
                is ApiResult.Success -> {
                    val serverBooks = result.data.map { it.toLocalBook() }
                    localDao.clear()
                    localDao.upsertAll(serverBooks)
                    _connectionState.value = ConnectionState.Online
                    SyncResult.Success
                }
                is ApiResult.Error -> {
                    _connectionState.value = ConnectionState.Error(result.message)
                    SyncResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Sync failed")
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun syncToServer(): SyncResult {
        if (!isAuthenticated()) {
            return SyncResult.Error("Not authenticated")
        }

        return try {
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

                if (result.isSuccess()) {
                    successful++
                    // Update local book with server data
                    val serverBook = result.getOrNull()?.toLocalBook()
                    if (serverBook != null) {
                        localDao.upsertAll(listOf(serverBook))
                    }
                } else {
                    failed++
                }
            }

            _connectionState.value = ConnectionState.Online

            if (failed == 0) {
                SyncResult.Success
            } else {
                SyncResult.Partial("Some books failed to sync", successful, failed)
            }

        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Sync failed")
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun replaceAllBooks(books: List<Book>) {
        localDao.clear()
        localDao.upsertAll(books)
    }

    private fun isAuthenticated(): Boolean {
        return _authState.value is AuthState.Authenticated
    }
}