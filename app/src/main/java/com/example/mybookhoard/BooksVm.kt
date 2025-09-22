package com.example.mybookhoard

import android.app.Application
import android.content.Context
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

@OptIn(FlowPreview::class)
class BooksVm(app: Application) : AndroidViewModel(app) {

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

    // Authentication methods
    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            repository.login(identifier, password)
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            repository.register(username, email, password)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            repository.getProfile()
        }
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

    // Book operations
    fun addBook(book: Book) {
        viewModelScope.launch {
            repository.addBook(book)
        }
    }

    fun addBooks(books: List<Book>) {
        viewModelScope.launch {
            books.forEach { repository.addBook(it) }
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            repository.updateBook(book)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
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
            repository.replaceAllBooks(list)
        }
    }

    // Import from assets (only for first run or offline mode)
    fun importFromAssetsOnce(ctx: Context) {
        viewModelScope.launch {
            // Only import if not authenticated and no local data
            if (authState.value !is AuthState.Authenticated) {
                val current = items.firstOrNull() ?: emptyList()
                if (current.isEmpty()) {
                    val csv = ctx.assets.open("libros_iniciales.csv")
                        .bufferedReader().use { it.readText() }
                    val books = parseCsv(csv)
                    repository.replaceAllBooks(books)
                }
            }
        }
    }

    // Sync operations
    fun syncFromServer() {
        viewModelScope.launch {
            repository.syncFromServer()
        }
    }

    fun syncToServer() {
        viewModelScope.launch {
            repository.syncToServer()
        }
    }

    // CSV parsing (kept for local imports)
    private fun parseCsv(csv: String): List<Book> {
        val reader = csvReader {
            skipEmptyLine = true
            autoRenameDuplicateHeaders = true
        }
        val rows = reader.readAllWithHeader(csv.byteInputStream())

        return rows.mapNotNull { r ->
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
}