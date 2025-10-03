package com.example.mybookhoard.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.books.BooksActionResult
import com.example.mybookhoard.api.books.UserBooksApiService
import com.example.mybookhoard.api.books.LibraryResult
import com.example.mybookhoard.api.books.UserBookResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.mybookhoard.repositories.UserBookRepository
import com.example.mybookhoard.repositories.BookRepository
import com.example.mybookhoard.data.entities.*
import com.example.mybookhoard.utils.BookSorting
import kotlinx.coroutines.FlowPreview
import com.example.mybookhoard.utils.FuzzySearchUtils
@OptIn(FlowPreview::class)
class LibraryViewModel(
    private val userBookRepository: UserBookRepository,
    private val bookRepository: BookRepository,
    private val userId: Long,
    private val userBooksApiService: UserBooksApiService,
) : ViewModel() {

    companion object {
        private const val TAG = "LibraryViewModel"
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Debounced search query (300ms delay)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    // Library statistics
    private val _libraryStats = MutableStateFlow(LibraryStats())
    val libraryStats: StateFlow<LibraryStats> = _libraryStats.asStateFlow()

    // Base data without filtering
    private val _baseReadBooks = MutableStateFlow<List<BookWithUserDataExtended>>(emptyList())
    private val _baseReadingBooks = MutableStateFlow<List<BookWithUserDataExtended>>(emptyList())
    private val _baseNotStartedBooks = MutableStateFlow<List<BookWithUserDataExtended>>(emptyList())

    // Filtered books by reading status (for My Library tab)
    // Filtered books by reading status (for My Library tab)
    val readBooks: StateFlow<List<BookWithUserDataExtended>> = combine(
        _baseReadBooks,
        debouncedSearchQuery
    ) { books, query ->
        if (query.isBlank()) books
        else FuzzySearchUtils.searchBooksExtended(books, query)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val readingBooks: StateFlow<List<BookWithUserDataExtended>> = combine(
        _baseReadingBooks,
        debouncedSearchQuery
    ) { books, query ->
        if (query.isBlank()) books
        else FuzzySearchUtils.searchBooksExtended(books, query)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val notStartedBooks: StateFlow<List<BookWithUserDataExtended>> = combine(
        _baseNotStartedBooks,
        debouncedSearchQuery
    ) { books, query ->
        if (query.isBlank()) books
        else FuzzySearchUtils.searchBooksExtended(books, query)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Books by wishlist status (for My Wishlist tab)
    private val _wishlistBooks = MutableStateFlow<List<BookWithUserDataExtended>>(emptyList())
    val wishlistBooks: StateFlow<List<BookWithUserDataExtended>> = _wishlistBooks.asStateFlow()

    private val _onTheWayBooks = MutableStateFlow<List<BookWithUserDataExtended>>(emptyList())
    val onTheWayBooks: StateFlow<List<BookWithUserDataExtended>> = _onTheWayBooks.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    data class LibraryStats(
        val totalBooks: Int = 0,
        val readBooks: Int = 0,
        val readingBooks: Int = 0,
        val notStartedBooks: Int = 0
    )

    init {
        loadLibraryData()
    }

    fun loadLibraryData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Optimized: Single API call that fetches everything
                when (val result = userBooksApiService.getUserBooksWithDetails(userId)) {
                    is LibraryResult.Success -> {
                        Log.d(TAG, "Loaded ${result.items.size} library items in single API call")

                        val allBooksWithUserData = result.items.map { it.toBookWithUserDataExtended() }

                        val obtainedBooks = allBooksWithUserData.filter {
                            it.userBook?.wishlistStatus == UserBookWishlistStatus.OBTAINED
                        }

                        _baseReadBooks.value = BookSorting.sortBySaga(
                            obtainedBooks.filter {
                                it.userBook?.readingStatus == UserBookReadingStatus.READ
                            }
                        )

                        _baseReadingBooks.value = BookSorting.sortBySaga(
                            obtainedBooks.filter {
                                it.userBook?.readingStatus == UserBookReadingStatus.READING
                            }
                        )

                        _baseNotStartedBooks.value = BookSorting.sortBySaga(
                            obtainedBooks.filter {
                                it.userBook?.readingStatus == UserBookReadingStatus.NOT_STARTED ||
                                        it.userBook?.readingStatus == null
                            }
                        )

                        _wishlistBooks.value = BookSorting.sortBySaga(
                            allBooksWithUserData.filter {
                                it.userBook?.wishlistStatus == UserBookWishlistStatus.WISH
                            }
                        )

                        _onTheWayBooks.value = BookSorting.sortBySaga(
                            allBooksWithUserData.filter {
                                it.userBook?.wishlistStatus == UserBookWishlistStatus.ON_THE_WAY
                            }
                        )

                        _libraryStats.value = LibraryStats(
                            totalBooks = obtainedBooks.size,
                            readBooks = _baseReadBooks.value.size,
                            readingBooks = _baseReadingBooks.value.size,
                            notStartedBooks = _baseNotStartedBooks.value.size
                        )

                        Log.d(TAG, "Library data loaded - Total: ${obtainedBooks.size}, " +
                                "Read: ${_baseReadBooks.value.size}, Reading: ${_baseReadingBooks.value.size}, " +
                                "Not Started: ${_baseNotStartedBooks.value.size}")
                    }

                    is LibraryResult.Error -> {
                        Log.e(TAG, "Error loading library data: ${result.message}")
                        _error.value = "Failed to load library: ${result.message}"
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception loading library data", e)
                _error.value = "Failed to load library: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }


    fun updateReadingStatus(bookId: Long, newStatus: UserBookReadingStatus) {
        viewModelScope.launch {
            try {
                // Find the UserBook that corresponds to the bookId
                val userBooks = userBooksApiService.getUserBooksForUser(userId)
                val userBook = userBooks.find { it.bookId == bookId }
                if (userBook == null) {
                    Log.w(TAG, "updateReadingStatus - no userBook found for bookId=$bookId")
                    return@launch
                }

                // Call the API
                val result = userBooksApiService.updateUserBookStatus(
                    userBookId = userBook.id,
                    newReading = newStatus,
                    newWishlist = userBook.wishlistStatus
                )
                if (result is UserBookResult.Success) {
                    Log.d(TAG, "updateReadingStatus success for bookId=$bookId")
                    loadLibraryData()
                } else {
                    _error.value = "Failed to update reading status"
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateReadingStatus exception", e)
                _error.value = "Failed to update reading status: ${e.message}"
            }
        }
    }

    fun updateWishlistStatus(bookId: Long, newStatus: UserBookWishlistStatus?) {
        viewModelScope.launch {
            try {
                // Find the UserBook that corresponds to the bookId
                val userBooks = userBooksApiService.getUserBooksForUser(userId)
                val userBook = userBooks.find { it.bookId == bookId }
                if (userBook == null) {
                    Log.w(TAG, "updateWishlistStatus - no userBook found for bookId=$bookId")
                    return@launch
                }

                // Call the API
                val result = userBooksApiService.updateUserBookStatus(
                    userBookId = userBook.id,
                    newReading = userBook.readingStatus,
                    newWishlist = newStatus
                )
                if (result is UserBookResult.Success) {
                    Log.d(TAG, "updateWishlistStatus success for bookId=$bookId")
                    loadLibraryData()
                } else {
                    _error.value = "Failed to update wishlist status"
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateWishlistStatus exception", e)
                _error.value = "Failed to update wishlist status: ${e.message}"
            }
        }
    }

    fun removeBookFromCollection(bookId: Long) {
        viewModelScope.launch {
            try {
                when (val result = userBooksApiService.removeBookFromCollection(bookId)) {
                    is BooksActionResult.Success -> {
                        Log.d(TAG, "Book removed from collection successfully: $bookId")
                        loadLibraryData()
                    }
                    is BooksActionResult.Error -> {
                        Log.e(TAG, "Failed to remove book: ${result.message}")
                        _error.value = "Failed to remove book: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception removing book from collection", e)
                _error.value = "Failed to remove book: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}