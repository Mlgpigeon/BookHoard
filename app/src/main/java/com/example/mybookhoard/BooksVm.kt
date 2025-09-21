package com.example.mybookhoard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.data.*
import com.example.mybookhoard.sync.SimplifiedGoogleDriveSync
import com.example.mybookhoard.utils.FuzzySearchUtils
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class BooksVm(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).bookDao()

    // 🆕 Google Drive sync service (disponible tras configurar google-services.json)
    val googleDriveSync = SimplifiedGoogleDriveSync(app)

    // Estado original de todos los libros
    val items: Flow<List<Book>> = dao.all()

    // Estados para búsqueda con debouncing automático
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

    // Libros filtrados por búsqueda normal (con debouncing)
    val filteredBooks: Flow<List<Book>> = combine(items, debouncedSearchQuery) { books, query ->
        if (query.isBlank()) {
            books
        } else {
            FuzzySearchUtils.searchBooksSimple(books, query, threshold = 0.25)
        }
    }

    // Libros de wishlist filtrados por búsqueda (con debouncing)
    val filteredWishlistBooks: Flow<List<Book>> = combine(items, debouncedWishlistSearchQuery) { books, query ->
        val wishlistBooks = books.filter {
            it.wishlist == WishlistStatus.WISH || it.wishlist == WishlistStatus.ON_THE_WAY
        }

        if (query.isBlank()) {
            wishlistBooks
        } else {
            FuzzySearchUtils.searchBooksSimple(wishlistBooks, query, threshold = 0.25)
        }
    }

    // Sugerencias de búsqueda (también con debouncing)
    fun getSearchSuggestions(query: String, isWishlist: Boolean = false): Flow<List<String>> {
        return if (isWishlist) {
            combine(items, debouncedWishlistSearchQuery) { books, _ ->
                if (query.length < 2) emptyList()
                else {
                    val wishlistBooks = books.filter {
                        it.wishlist == WishlistStatus.WISH || it.wishlist == WishlistStatus.ON_THE_WAY
                    }
                    FuzzySearchUtils.generateSearchSuggestions(wishlistBooks, query)
                }
            }
        } else {
            combine(items, debouncedSearchQuery) { books, _ ->
                if (query.length < 2) emptyList()
                else FuzzySearchUtils.generateSearchSuggestions(books, query)
            }
        }
    }

    // Función para buscar autores con fuzzy search
    fun searchAuthorSuggestions(query: String): Flow<List<String>> {
        return if (query.length < 2) {
            flowOf(emptyList())
        } else {
            dao.getUniqueAuthors().map { authors ->
                FuzzySearchUtils.searchAuthorsSimple(authors, query, threshold = 0.3)
            }
        }
    }

    // Nueva función para buscar sagas con fuzzy search
    fun searchSagaSuggestions(query: String): Flow<List<String>> {
        return if (query.length < 2) {
            flowOf(emptyList())
        } else {
            dao.getUniqueSagas().map { sagas ->
                FuzzySearchUtils.searchAuthorsSimple(sagas, query, threshold = 0.3)
            }
        }
    }

    // Métodos para actualizar queries (instantáneos en UI, pero debounced en filtrado)
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

    fun replaceAll(list: List<Book>) {
        viewModelScope.launch {
            dao.clear()
            dao.upsertAll(list)
        }
    }

    fun addBook(book: Book) {
        viewModelScope.launch {
            dao.upsertAll(listOf(book))
        }
    }

    fun addBooks(books: List<Book>) {
        viewModelScope.launch {
            dao.upsertAll(books)
        }
    }

    /** Importa el CSV de assets solo si la DB está vacía */
    fun importFromAssetsOnce(ctx: Context) {
        viewModelScope.launch {
            val current = dao.all().firstOrNull() ?: emptyList()
            if (current.isEmpty()) {
                val csv = ctx.assets.open("libros_iniciales.csv")
                    .bufferedReader().use { it.readText() }
                val books = parseCsv(csv)
                dao.upsertAll(books)
            }
        }
    }

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

            // Parse wishlist status from CSV
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

    fun updateStatus(book: Book, status: ReadingStatus) {
        viewModelScope.launch {
            val updated = book.copy(status = status)
            dao.upsertAll(listOf(updated))
        }
    }

    fun updateWishlist(book: Book, status: WishlistStatus?) {
        viewModelScope.launch {
            val updated = book.copy(wishlist = status)
            dao.upsertAll(listOf(updated))
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            dao.upsertAll(listOf(book))
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            dao.delete(book)
        }
    }

    fun searchBooks(query: String): Flow<List<Book>> {
        return if (query.isBlank()) {
            dao.all()
        } else {
            dao.searchBooks("%$query%")
        }
    }

    fun getBookById(id: Long): Flow<Book?> {
        return dao.getBookById(id)
    }

    // 🆕 Sync methods (disponibles tras configurar Google Services)

    fun uploadToCloud() {
        viewModelScope.launch {
            val books = items.firstOrNull() ?: emptyList()
            googleDriveSync.uploadBooks(books)
        }
    }

    fun downloadFromCloud() {
        viewModelScope.launch {
            val books = googleDriveSync.downloadBooks()
            if (books != null) {
                replaceAll(books)
            }
        }
    }

    fun signOutFromCloud() {
        viewModelScope.launch {
            googleDriveSync.signOut()
        }
    }
}