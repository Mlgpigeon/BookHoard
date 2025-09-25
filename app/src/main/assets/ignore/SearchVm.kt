package com.example.mybookhoard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.data.Book
import com.example.mybookhoard.repository.BookRepository
import com.example.mybookhoard.utils.FuzzySearchUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import com.example.mybookhoard.api.ApiResult
import com.example.mybookhoard.api.CombinedSearchResponse
import com.example.mybookhoard.api.SearchResult
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchVm(private val repository: BookRepository) : ViewModel() {

    // Search states with debouncing
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _wishlistSearchQuery = MutableStateFlow("")
    val wishlistSearchQuery: StateFlow<String> = _wishlistSearchQuery

    // Google Books search state
    private val _googleSearchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val googleSearchResults: StateFlow<List<SearchResult>> = _googleSearchResults

    private val _isSearchingGoogle = MutableStateFlow(false)
    val isSearchingGoogle: StateFlow<Boolean> = _isSearchingGoogle

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    // Combined search results (local + Google Books)
    private val _combinedSearchResults = MutableStateFlow<CombinedSearchResponse?>(null)
    val combinedSearchResults: StateFlow<CombinedSearchResponse?> = _combinedSearchResults


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

    fun searchWithGoogleBooks(query: String) {
        if (query.isBlank()) {
            _googleSearchResults.value = emptyList()
            _combinedSearchResults.value = null
            return
        }

        viewModelScope.launch {
            _isSearchingGoogle.value = true
            _searchError.value = null

            try {
                when (val result = repository.searchBooksWithGoogleBooks(query)) {
                    is ApiResult.Success -> {
                        _combinedSearchResults.value = result.data
                        _googleSearchResults.value = result.data.googleResults
                        _searchError.value = null
                    }
                    is ApiResult.Error -> {
                        _searchError.value = result.message
                        _googleSearchResults.value = emptyList()
                        _combinedSearchResults.value = null
                    }
                }
            } catch (e: Exception) {
                _searchError.value = "Search failed: ${e.message}"
                _googleSearchResults.value = emptyList()
                _combinedSearchResults.value = null
            } finally {
                _isSearchingGoogle.value = false
            }
        }
    }

    // Clear Google search results
    fun clearGoogleSearch() {
        _googleSearchResults.value = emptyList()
        _combinedSearchResults.value = null
        _searchError.value = null
    }

    fun getCombinedResults(): Flow<List<SearchResult>> {
        return combine(
            filteredBooks,
            combinedSearchResults
        ) { localBooks, combinedResults ->
            val localSearchResults = localBooks.map { SearchResult.fromLocalBook(it) }

            if (combinedResults != null) {
                // Combine local results with Google Books results
                localSearchResults + combinedResults.googleResults
            } else {
                localSearchResults
            }
        }
    }

}