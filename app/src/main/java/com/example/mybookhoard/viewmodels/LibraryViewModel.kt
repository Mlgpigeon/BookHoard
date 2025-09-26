package com.example.mybookhoard.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.books.UserBooksApiService
import com.example.mybookhoard.api.books.LibraryResult
import com.example.mybookhoard.api.books.UserBookResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.mybookhoard.repositories.UserBookRepository
import com.example.mybookhoard.repositories.BookRepository
import com.example.mybookhoard.data.entities.*

class LibraryViewModel(
    private val userBookRepository: UserBookRepository,
    private val bookRepository: BookRepository,
    private val userId: Long,
    private val userBooksApiService: UserBooksApiService,
) : ViewModel() {

    companion object {
        private const val TAG = "LibraryViewModel"
    }

    // Library statistics
    private val _libraryStats = MutableStateFlow(LibraryStats())
    val libraryStats: StateFlow<LibraryStats> = _libraryStats.asStateFlow()

    // Books by reading status (for My Library tab)
    private val _readBooks = MutableStateFlow<List<BookWithUserData>>(emptyList())
    val readBooks: StateFlow<List<BookWithUserData>> = _readBooks.asStateFlow()

    private val _readingBooks = MutableStateFlow<List<BookWithUserData>>(emptyList())
    val readingBooks: StateFlow<List<BookWithUserData>> = _readingBooks.asStateFlow()

    private val _notStartedBooks = MutableStateFlow<List<BookWithUserData>>(emptyList())
    val notStartedBooks: StateFlow<List<BookWithUserData>> = _notStartedBooks.asStateFlow()

    // Books by wishlist status (for My Wishlist tab)
    private val _wishlistBooks = MutableStateFlow<List<BookWithUserData>>(emptyList())
    val wishlistBooks: StateFlow<List<BookWithUserData>> = _wishlistBooks.asStateFlow()

    private val _onTheWayBooks = MutableStateFlow<List<BookWithUserData>>(emptyList())
    val onTheWayBooks: StateFlow<List<BookWithUserData>> = _onTheWayBooks.asStateFlow()

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

                        // Convert LibraryItems to BookWithUserData
                        val allBooksWithUserData = result.items.map { it.toBookWithUserData() }

                        // Filter by states - Only OBTAINED books go to "My Library"
                        val obtainedBooks = allBooksWithUserData.filter {
                            it.userBook?.wishlistStatus == UserBookWishlistStatus.OBTAINED
                        }

                        _readBooks.value = obtainedBooks.filter {
                            it.userBook?.readingStatus == UserBookReadingStatus.READ
                        }

                        _readingBooks.value = obtainedBooks.filter {
                            it.userBook?.readingStatus == UserBookReadingStatus.READING
                        }

                        _notStartedBooks.value = obtainedBooks.filter {
                            it.userBook?.readingStatus == UserBookReadingStatus.NOT_STARTED ||
                                    it.userBook?.readingStatus == null
                        }

                        // Data for Wishlist tab
                        _wishlistBooks.value = allBooksWithUserData.filter {
                            it.userBook?.wishlistStatus == UserBookWishlistStatus.WISH
                        }

                        _onTheWayBooks.value = allBooksWithUserData.filter {
                            it.userBook?.wishlistStatus == UserBookWishlistStatus.ON_THE_WAY
                        }

                        // Updated statistics
                        _libraryStats.value = LibraryStats(
                            totalBooks = obtainedBooks.size,
                            readBooks = _readBooks.value.size,
                            readingBooks = _readingBooks.value.size,
                            notStartedBooks = _notStartedBooks.value.size
                        )

                        Log.d(TAG, "Library data loaded efficiently - Total: ${obtainedBooks.size}, " +
                                "Read: ${_readBooks.value.size}, Reading: ${_readingBooks.value.size}, " +
                                "Not Started: ${_notStartedBooks.value.size}, " +
                                "Wishlist: ${_wishlistBooks.value.size}, On The Way: ${_onTheWayBooks.value.size}")
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
                val userBooks = userBooksApiService.getUserBooksForUser(userId)
                val userBook = userBooks.find { it.bookId == bookId }
                if (userBook == null) {
                    Log.w(TAG, "updateWishlistStatus - no userBook found for bookId=$bookId")
                    return@launch
                }

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
                userBookRepository.deleteUserBook(userId, bookId)
                Log.d(TAG, "Removed book $bookId from collection")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing book from collection", e)
                _error.value = "Failed to remove book: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}