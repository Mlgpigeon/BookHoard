package com.example.mybookhoard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.mybookhoard.api.books.BooksApiService
import com.example.mybookhoard.api.books.BooksSearchResult
import com.example.mybookhoard.api.books.ApiBook
import com.example.mybookhoard.api.books.BooksActionResult
import com.example.mybookhoard.data.entities.*

class SearchViewModel(
    private val booksApiService: BooksApiService,
    private val currentUserId: Long
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Suggestions state
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Debounce job for suggestions
    private var suggestionsJob: Job? = null

    // Last search query to enable retry
    private var lastSearchQuery: String = ""

    sealed class SearchUiState {
        object Initial : SearchUiState()
        object Loading : SearchUiState()
        data class Success(val books: List<BookWithUserDataExtended>) : SearchUiState()
        data class Error(val message: String) : SearchUiState()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        // Clear results if query is empty
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Initial
            _suggestions.value = emptyList()
            return
        }

        // Generate suggestions with debounce
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            delay(300) // Debounce
            if (query.length >= 2 && query == _searchQuery.value) {
                generateSuggestions(query)
            }
        }
    }

    fun selectSuggestion(suggestion: String) {
        _searchQuery.value = suggestion
        _suggestions.value = emptyList()
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        lastSearchQuery = query
        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            // Search in API - API includes user collection status
            when (val result = booksApiService.searchBooks(query, includeGoogleBooks = false)) {
                is BooksSearchResult.Success -> {
                    // Convert API books to BookWithUserDataExtended
                    val booksWithUserData = result.books.map { apiBook ->
                        val book = apiBook.toBookEntity()
                        // Create UserBook if already in collection (canBeAdded = false)
                        val userBook = if (apiBook.canBeAdded == false) {
                            // If canBeAdded is false, it means the book is in user's collection
                            UserBook(
                                userId = currentUserId,
                                bookId = book.id,
                                readingStatus = UserBookReadingStatus.NOT_STARTED,
                                wishlistStatus = UserBookWishlistStatus.WISH // Default, could be improved
                            )
                        } else null

                        BookWithUserDataExtended(
                            book = book,
                            userBook = userBook,
                            authorName = apiBook.author,
                            sagaName = apiBook.saga,
                            sourceLabel = apiBook.sourceLabel
                        )
                    }

                    _uiState.value = SearchUiState.Success(booksWithUserData)
                }
                is BooksSearchResult.Error -> {
                    _uiState.value = SearchUiState.Error(result.message)
                }
            }
        }
    }

    fun retrySearch() {
        if (lastSearchQuery.isNotBlank()) {
            performSearch()
        }
    }

    fun addBookToCollection(book: Book, wishlistStatus: UserBookWishlistStatus) {
        viewModelScope.launch {
            try {
                // Call the corrected API method that creates user_book relationship
                when (val result = booksApiService.addBookToCollection(
                    bookId = book.id,
                    wishlistStatus = wishlistStatus.name
                )) {
                    is BooksActionResult.Success -> {
                        // Refresh search results to update the button state
                        if (lastSearchQuery.isNotBlank()) {
                            performSearch()
                        }
                    }
                    is BooksActionResult.Error -> {
                        _uiState.value = SearchUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    "Failed to add book to collection: ${e.message}"
                )
            }
        }
    }

    fun removeBookFromCollection(bookId: Long) {
        viewModelScope.launch {
            try {
                when (val result = booksApiService.removeBookFromCollection(bookId)) {
                    is BooksActionResult.Success -> {
                        // Refresh search results to update the button state
                        if (lastSearchQuery.isNotBlank()) {
                            performSearch()
                        }
                    }
                    is BooksActionResult.Error -> {
                        _uiState.value = SearchUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    "Failed to remove book from collection: ${e.message}"
                )
            }
        }
    }

    private suspend fun generateSuggestions(query: String) {
        try {
            // Get suggestions from API search
            when (val result = booksApiService.searchBooks(query, includeGoogleBooks = false)) {
                is BooksSearchResult.Success -> {
                    val suggestions = result.books
                        .take(5) // Limit suggestions
                        .mapNotNull { book ->
                            when {
                                book.title.contains(query, ignoreCase = true) -> book.title
                                book.author?.contains(query, ignoreCase = true) == true -> book.author
                                book.genres?.any { it.contains(query, ignoreCase = true) } == true ->
                                    book.genres.find { it.contains(query, ignoreCase = true) }
                                else -> null
                            }
                        }
                        .distinct()

                    _suggestions.value = suggestions
                }
                is BooksSearchResult.Error -> {
                    // Silently fail for suggestions
                    _suggestions.value = emptyList()
                }
            }
        } catch (e: Exception) {
            // Silently fail for suggestions
            _suggestions.value = emptyList()
        }
    }
}