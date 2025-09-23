package com.example.mybookhoard.viewmodels

import androidx.lifecycle.ViewModel
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.data.WishlistStatus
import com.example.mybookhoard.repository.BookRepository
import com.example.mybookhoard.utils.FuzzySearchUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
class SearchVm(private val repository: BookRepository) : ViewModel() {

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

    // All books from repository
    val items: Flow<List<Book>> = repository.getAllBooks()

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
}