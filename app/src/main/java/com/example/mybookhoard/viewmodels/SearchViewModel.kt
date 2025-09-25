package com.example.mybookhoard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.mybookhoard.data.entities.*
import com.example.mybookhoard.repositories.BookRepository
import com.example.mybookhoard.repositories.UserBookRepository
import com.example.mybookhoard.utils.FuzzySearchUtils
import java.util.Date

class SearchViewModel(
    private val bookRepository: BookRepository,
    private val userBookRepository: UserBookRepository,
    private val currentUserId: Long // TODO: Inject from auth
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

    init {
        // Set up suggestion generation based on query changes
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank() && query.length >= 2) {
                        generateSuggestions(query)
                    } else {
                        _suggestions.value = emptyList()
                    }
                }
        }
    }

    sealed class SearchUiState {
        object Initial : SearchUiState()
        object Loading : SearchUiState()
        data class Success(val books: List<BookWithUserData>) : SearchUiState()
        data class Error(val message: String) : SearchUiState()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        // Clear results if query is empty
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Initial
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
            try {
                // Search in all public books
                val publicBooks = bookRepository.searchPublicBooks(query).first()

                // Apply fuzzy search
                val filteredBooks = FuzzySearchUtils.searchPublicBooks(publicBooks, query)

                // Convert to BookWithUserData by checking if user has each book
                val booksWithUserData = filteredBooks.map { book ->
                    val userBook = userBookRepository.getUserBookSync(currentUserId, book.id)
                    BookWithUserData(book, userBook)
                }

                _uiState.value = SearchUiState.Success(booksWithUserData)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    e.message ?: "An error occurred while searching"
                )
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
                val userBook = UserBook(
                    userId = currentUserId,
                    bookId = book.id,
                    readingStatus = UserBookReadingStatus.NOT_STARTED,
                    wishlistStatus = wishlistStatus,
                    createdAt = Date(),
                    updatedAt = Date()
                )

                userBookRepository.addUserBook(userBook)

                // Refresh search results
                if (lastSearchQuery.isNotBlank()) {
                    performSearch()
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
                userBookRepository.deleteUserBook(currentUserId, bookId)

                // Refresh search results
                if (lastSearchQuery.isNotBlank()) {
                    performSearch()
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    "Failed to remove book from collection: ${e.message}"
                )
            }
        }
    }

    private fun generateSuggestions(query: String) {
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            try {
                // Get public books for suggestion generation
                bookRepository.getPublicBooks()
                    .first()
                    .let { publicBooks ->
                        val suggestions = FuzzySearchUtils.generateSuggestions(publicBooks, query)
                        _suggestions.value = suggestions
                    }
            } catch (e: Exception) {
                // Silently fail for suggestions
                _suggestions.value = emptyList()
            }
        }
    }
}