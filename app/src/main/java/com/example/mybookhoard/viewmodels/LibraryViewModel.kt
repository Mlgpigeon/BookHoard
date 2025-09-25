package com.example.mybookhoard.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.mybookhoard.repositories.UserBookRepository
import com.example.mybookhoard.repositories.BookRepository
import com.example.mybookhoard.data.entities.*

class LibraryViewModel(
    private val userBookRepository: UserBookRepository,
    private val bookRepository: BookRepository,
    private val userId: Long
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
                // Load all user books with book data
                userBookRepository.getBooksWithUserData(userId).collect { allBooksWithUserData ->

                    // Filter for obtained books (My Library)
                    val obtainedBooks = allBooksWithUserData.filter {
                        it.userBook?.wishlistStatus == UserBookWishlistStatus.OBTAINED
                    }

                    // Separate by reading status
                    _readBooks.value = obtainedBooks.filter {
                        it.userBook?.readingStatus == UserBookReadingStatus.READ
                    }
                    _readingBooks.value = obtainedBooks.filter {
                        it.userBook?.readingStatus == UserBookReadingStatus.READING
                    }
                    _notStartedBooks.value = obtainedBooks.filter {
                        it.userBook?.readingStatus == UserBookReadingStatus.NOT_STARTED
                    }

                    // Filter for wishlist books (My Wishlist)
                    _wishlistBooks.value = allBooksWithUserData.filter {
                        it.userBook?.wishlistStatus == UserBookWishlistStatus.WISH
                    }
                    _onTheWayBooks.value = allBooksWithUserData.filter {
                        it.userBook?.wishlistStatus == UserBookWishlistStatus.ON_THE_WAY
                    }

                    // Calculate statistics
                    _libraryStats.value = LibraryStats(
                        totalBooks = obtainedBooks.size,
                        readBooks = _readBooks.value.size,
                        readingBooks = _readingBooks.value.size,
                        notStartedBooks = _notStartedBooks.value.size
                    )

                    Log.d(TAG, "Library data loaded - Total: ${obtainedBooks.size}, " +
                            "Read: ${_readBooks.value.size}, Reading: ${_readingBooks.value.size}, " +
                            "Not Started: ${_notStartedBooks.value.size}, " +
                            "Wishlist: ${_wishlistBooks.value.size}, On The Way: ${_onTheWayBooks.value.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading library data", e)
                _error.value = "Failed to load library: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateReadingStatus(bookId: Long, newStatus: UserBookReadingStatus) {
        viewModelScope.launch {
            try {
                val userBook = userBookRepository.getUserBookSync(userId, bookId)
                if (userBook != null) {
                    val updatedUserBook = userBook.copy(
                        readingStatus = newStatus,
                        dateStarted = if (newStatus == UserBookReadingStatus.READING && userBook.dateStarted == null)
                            java.util.Date() else userBook.dateStarted,
                        dateFinished = if (newStatus == UserBookReadingStatus.READ)
                            java.util.Date() else null
                    )
                    userBookRepository.updateUserBook(updatedUserBook)
                    Log.d(TAG, "Updated reading status for book $bookId to $newStatus")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating reading status", e)
                _error.value = "Failed to update reading status: ${e.message}"
            }
        }
    }

    fun updateWishlistStatus(bookId: Long, newStatus: UserBookWishlistStatus?) {
        viewModelScope.launch {
            try {
                val userBook = userBookRepository.getUserBookSync(userId, bookId)
                if (userBook != null) {
                    val updatedUserBook = userBook.copy(wishlistStatus = newStatus)
                    userBookRepository.updateUserBook(updatedUserBook)
                    Log.d(TAG, "Updated wishlist status for book $bookId to $newStatus")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating wishlist status", e)
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