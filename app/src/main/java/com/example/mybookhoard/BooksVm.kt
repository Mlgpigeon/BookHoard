package com.example.mybookhoard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import com.example.mybookhoard.repository.BookRepository
import com.example.mybookhoard.utils.FuzzySearchUtils
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(FlowPreview::class)
class BooksVm(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "BooksVm"
    }

    private val repository = BookRepository(app)

    // Authentication state
    val authState: StateFlow<AuthState> = repository.authState
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    // All books from repository
    val items: Flow<List<Book>> = repository.getAllBooks()

    // Search states with debouncing
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _wishlistSearchQuery = MutableStateFlow("")
    val wishlistSearchQuery: StateFlow<String> = _wishlistSearchQuery

    // Debounced search queries (300ms delay)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    private val debouncedWishlistSearchQuery = _wishlistSearchQuery
        .debounce(300)
        .distinctUntilChanged()

    // Filtered books using repository search
    val filteredBooks: Flow<List<Book>> = combine(items, debouncedSearchQuery) { books, query ->
        if (query.isBlank()) {
            books
        } else {
            FuzzySearchUtils.searchBooksSimple(books, query, threshold = 0.25)
        }
    }

    // Wishlist books with search
    val filteredWishlistBooks: Flow<List<Book>> = combine(
        repository.getWishlistBooks(),
        debouncedWishlistSearchQuery
    ) { books, query ->
        if (query.isBlank()) {
            books
        } else {
            FuzzySearchUtils.searchBooksSimple(books, query, threshold = 0.25)
        }
    }

    // Authentication methods with improved error handling
    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            Log.w(TAG, "Login attempt with empty credentials")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting login for: $identifier")
                val result = repository.login(identifier, password)

                when (result) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "Login successful for: ${result.user.username}")
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "Login failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                // Set error state if repository didn't handle it
                if (authState.value !is AuthState.Error) {
                    repository.authState.let { state ->
                        if (state is MutableStateFlow) {
                            state.value = AuthState.Error("Login failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            Log.w(TAG, "Registration attempt with empty fields")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting registration for: $username")
                val result = repository.register(username, email, password)

                when (result) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "Registration successful for: ${result.user.username}")
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "Registration failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
                // Set error state if repository didn't handle it
                if (authState.value !is AuthState.Error) {
                    repository.authState.let { state ->
                        if (state is MutableStateFlow) {
                            state.value = AuthState.Error("Registration failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting logout")
                repository.logout()
                Log.d(TAG, "Logout successful")
            } catch (e: Exception) {
                Log.e(TAG, "Logout error: ${e.message}", e)
            }
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            try {
                repository.getProfile()
            } catch (e: Exception) {
                Log.e(TAG, "Get profile error: ${e.message}", e)
            }
        }
    }

    // Test connectivity
    fun testConnection() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing connection...")
                // This would use a new method in repository/apiService for health check
                // For now, we'll just try to get profile if authenticated
                if (isAuthenticated()) {
                    repository.getProfile()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection test error: ${e.message}", e)
            }
        }
    }

    // Clear error state
    fun clearAuthError() {
        repository.clearAuthError()
    }

    fun clearConnectionError() {
        repository.clearConnectionError()
    }

    // Search suggestions
    fun getSearchSuggestions(query: String, isWishlist: Boolean = false): Flow<List<String>> {
        return if (isWishlist) {
            combine(repository.getWishlistBooks(), debouncedWishlistSearchQuery) { books, _ ->
                if (query.length < 2) emptyList()
                else FuzzySearchUtils.generateSearchSuggestions(books, query)
            }
        } else {
            combine(items, debouncedSearchQuery) { books, _ ->
                if (query.length < 2) emptyList()
                else FuzzySearchUtils.generateSearchSuggestions(books, query)
            }
        }
    }

    fun searchAuthorSuggestions(query: String): Flow<List<String>> {
        return if (query.length < 2) {
            flowOf(emptyList())
        } else {
            repository.getUniqueAuthors().map { authors ->
                FuzzySearchUtils.searchAuthorsSimple(authors, query, threshold = 0.3)
            }
        }
    }

    fun searchSagaSuggestions(query: String): Flow<List<String>> {
        return if (query.length < 2) {
            flowOf(emptyList())
        } else {
            repository.getUniqueSagas().map { sagas ->
                FuzzySearchUtils.searchAuthorsSimple(sagas, query, threshold = 0.3)
            }
        }
    }

    // Search query updates
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateWishlistSearchQuery(query: String) {
        _wishlistSearchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun clearWishlistSearch() {
        _wishlistSearchQuery.value = ""
    }

    // Book operations with improved error handling
    fun addBook(book: Book) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding book: ${book.title}")
                val success = repository.addBook(book)
                if (success) {
                    Log.d(TAG, "Book added successfully: ${book.title}")
                } else {
                    Log.w(TAG, "Book added locally only: ${book.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding book '${book.title}': ${e.message}", e)
            }
        }
    }

    fun addBooks(books: List<Book>) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding ${books.size} books")
                books.forEach { book ->
                    try {
                        repository.addBook(book)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding book '${book.title}': ${e.message}")
                    }
                }
                Log.d(TAG, "Finished adding books")
            } catch (e: Exception) {
                Log.e(TAG, "Error in bulk add books: ${e.message}", e)
            }
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating book: ${book.title}")
                val success = repository.updateBook(book)
                if (success) {
                    Log.d(TAG, "Book updated successfully: ${book.title}")
                } else {
                    Log.w(TAG, "Book updated locally only: ${book.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating book '${book.title}': ${e.message}", e)
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting book: ${book.title}")
                val success = repository.deleteBook(book)
                if (success) {
                    Log.d(TAG, "Book deleted successfully: ${book.title}")
                } else {
                    Log.w(TAG, "Book deleted locally only: ${book.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting book '${book.title}': ${e.message}", e)
            }
        }
    }

    fun updateStatus(book: Book, status: ReadingStatus) {
        val updated = book.copy(status = status)
        updateBook(updated)
    }

    fun updateWishlist(book: Book, status: WishlistStatus?) {
        val updated = book.copy(wishlist = status)
        updateBook(updated)
    }

    fun getBookById(id: Long): Flow<Book?> {
        return repository.getBookById(id)
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
    fun importFromAssetsOnce(ctx: Context) {
        viewModelScope.launch {
            try {
                // Only import if not authenticated and no local data
                if (authState.value !is AuthState.Authenticated) {
                    val current = items.firstOrNull() ?: emptyList()
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

    // Auto-retry mechanism for network operations
    fun retryNetworkOperation() {
        viewModelScope.launch {
            Log.d(TAG, "Retrying network operation...")
            delay(1000) // Brief delay before retry

            if (isAuthenticated()) {
                syncFromServer()
            } else {
                testConnection()
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

    // Connection status helpers
    fun isOnline(): Boolean = connectionState.value is ConnectionState.Online
    fun isOffline(): Boolean = connectionState.value is ConnectionState.Offline
    fun isSyncing(): Boolean = connectionState.value is ConnectionState.Syncing
    fun hasConnectionError(): Boolean = connectionState.value is ConnectionState.Error

    // Auth status helpers
    fun isAuthenticated(): Boolean = authState.value is AuthState.Authenticated
    fun isAuthenticating(): Boolean = authState.value is AuthState.Authenticating
    fun hasAuthError(): Boolean = authState.value is AuthState.Error

    fun getCurrentUser(): User? {
        return when (val state = authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    fun getAuthErrorMessage(): String? {
        return when (val state = authState.value) {
            is AuthState.Error -> state.message
            else -> null
        }
    }

    fun getConnectionErrorMessage(): String? {
        return when (val state = connectionState.value) {
            is ConnectionState.Error -> state.message
            else -> null
        }
    }
}