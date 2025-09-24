package com.example.mybookhoard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import com.example.mybookhoard.repository.BookRepository
import com.example.mybookhoard.repository.UserBookRepository
import com.example.mybookhoard.viewmodels.AuthVm
import com.example.mybookhoard.viewmodels.SearchVm
import com.example.mybookhoard.viewmodels.SyncVm
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class BooksVm(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "BooksVm"
    }

    private val repository = BookRepository(app)

    private val userBookRepository = UserBookRepository(app)

    // Modular ViewModels
    val authVm = AuthVm(repository)
    val searchVm = SearchVm(repository)
    val syncVm = SyncVm(repository)

    // Expose auth state through delegation
    val authState: StateFlow<AuthState> = authVm.authState
    val connectionState: StateFlow<ConnectionState> = authVm.connectionState

    // Expose search functionality
    val items: Flow<List<Book>> = searchVm.items
    val searchQuery: StateFlow<String> = searchVm.searchQuery
    val wishlistSearchQuery: StateFlow<String> = searchVm.wishlistSearchQuery
    val filteredBooks: Flow<List<Book>> = searchVm.filteredBooks
    val filteredWishlistBooks: Flow<List<Book>> = searchVm.filteredWishlistBooks

    // Auth methods delegation
    fun login(identifier: String, password: String) = authVm.login(identifier, password)
    fun register(username: String, email: String, password: String) = authVm.register(username, email, password)
    fun logout() = authVm.logout()
    fun getProfile() = authVm.getProfile()
    fun testConnection() = authVm.testConnection()
    fun clearAuthError() = authVm.clearAuthError()
    fun clearConnectionError() = authVm.clearConnectionError()
    fun retryNetworkOperation() = authVm.retryNetworkOperation()

    // Search methods delegation
    fun updateSearchQuery(query: String) = searchVm.updateSearchQuery(query)
    fun updateWishlistSearchQuery(query: String) = searchVm.updateWishlistSearchQuery(query)
    fun clearSearch() = searchVm.clearSearch()
    fun clearWishlistSearch() = searchVm.clearWishlistSearch()
    fun getSearchSuggestions(query: String, isWishlist: Boolean = false) = searchVm.getSearchSuggestions(query, isWishlist)
    fun searchAuthorSuggestions(query: String) = searchVm.searchAuthorSuggestions(query)
    fun searchSagaSuggestions(query: String) = searchVm.searchSagaSuggestions(query)

    fun searchWithGoogleBooks(query: String) = searchVm.searchWithGoogleBooks(query)
    fun clearGoogleSearch() = searchVm.clearGoogleSearch()
    fun getCombinedResults() = searchVm.getCombinedResults()

    // Sync methods delegation
    fun syncFromServer() = syncVm.syncFromServer()
    fun syncToServer() = syncVm.syncToServer()
    fun replaceAll(list: List<Book>) = syncVm.replaceAll(list)
    fun importFromAssetsOnce(ctx: Context) = syncVm.importFromAssetsOnce(ctx, authVm)

    // Status helpers delegation
    fun isOnline(): Boolean = authVm.isOnline()
    fun isOffline(): Boolean = authVm.isOffline()
    fun isSyncing(): Boolean = authVm.isSyncing()
    fun hasConnectionError(): Boolean = authVm.hasConnectionError()
    fun isAuthenticated(): Boolean = authVm.isAuthenticated()
    fun isAuthenticating(): Boolean = authVm.isAuthenticating()
    fun hasAuthError(): Boolean = authVm.hasAuthError()
    fun getCurrentUser(): User? = authVm.getCurrentUser()
    fun getAuthErrorMessage(): String? = authVm.getAuthErrorMessage()
    fun getConnectionErrorMessage(): String? = authVm.getConnectionErrorMessage()

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

    fun addGoogleBook(
        title: String,
        author: String? = null,
        saga: String? = null,
        description: String? = null,
        wishlistStatus: WishlistStatus  // Keep existing parameter name for compatibility
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding Google Book as UserBook: $title with wishlist status: ${wishlistStatus.name}")

                val currentUser = getCurrentUser()
                if (currentUser == null) {
                    Log.e(TAG, "Cannot add book: User not authenticated")
                    return@launch
                }

                // Convert WishlistStatus to UserBookWishlistStatus
                val userBookWishlistStatus = when (wishlistStatus) {
                    WishlistStatus.WISH -> UserBookWishlistStatus.WISH
                    WishlistStatus.ON_THE_WAY -> UserBookWishlistStatus.ON_THE_WAY
                    WishlistStatus.OBTAINED -> UserBookWishlistStatus.OBTAINED
                }

                val success = userBookRepository.addGoogleBookAsUserBook(
                    title = title,
                    author = author,
                    saga = saga,
                    description = description,
                    wishlistStatus = userBookWishlistStatus
                )

                if (success) {
                    Log.d(TAG, "Google Book added successfully as UserBook: $title")
                    // The UserBook will automatically appear in the UI through the repository flow
                } else {
                    Log.e(TAG, "Failed to add Google Book as UserBook: $title")
                    // TODO: Show error message to user
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error adding Google Book '$title' as UserBook: ${e.message}", e)
            }
        }
    }

    fun getUserBooks(): Flow<List<UserBook>> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            userBookRepository.getUserBooks(currentUser.id)
        } else {
            flowOf(emptyList())
        }
    }

    fun getAllUserBooks(): Flow<List<UserBook>> {
        return userBookRepository.getAllUserBooks()
    }

    fun getUserBookById(userBookId: Long): Flow<UserBook?> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            userBookRepository.getUserBookById(userBookId, currentUser.id)
        } else {
            flowOf(null)
        }
    }

    fun updateUserBookStatus(userBook: UserBook, status: UserBookReadingStatus) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating UserBook status: ${userBook.id} to ${status.name}")
                val updated = userBook.copy(
                    readingStatus = status,
                    updatedAt = Date()
                )
                val success = userBookRepository.updateUserBook(updated)
                if (success) {
                    Log.d(TAG, "UserBook status updated successfully")
                } else {
                    Log.w(TAG, "UserBook status update failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UserBook status: ${e.message}", e)
            }
        }
    }

    fun updateUserBookWishlist(userBook: UserBook, status: UserBookWishlistStatus?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating UserBook wishlist: ${userBook.id} to ${status?.name}")
                val updated = userBook.copy(
                    wishlistStatus = status,
                    updatedAt = Date()
                )
                val success = userBookRepository.updateUserBook(updated)
                if (success) {
                    Log.d(TAG, "UserBook wishlist updated successfully")
                } else {
                    Log.w(TAG, "UserBook wishlist update failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UserBook wishlist: ${e.message}", e)
            }
        }
    }

    fun updateUserBookRating(userBook: UserBook, rating: Int?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating UserBook rating: ${userBook.id} to $rating")
                val updated = userBook.copy(
                    personalRating = rating,
                    updatedAt = Date()
                )
                val success = userBookRepository.updateUserBook(updated)
                if (success) {
                    Log.d(TAG, "UserBook rating updated successfully")
                } else {
                    Log.w(TAG, "UserBook rating update failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UserBook rating: ${e.message}", e)
            }
        }
    }

    fun updateUserBookProgress(userBook: UserBook, progress: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating UserBook progress: ${userBook.id} to $progress%")
                val updated = userBook.copy(
                    readingProgress = progress.coerceIn(0, 100),
                    updatedAt = Date()
                )
                val success = userBookRepository.updateUserBook(updated)
                if (success) {
                    Log.d(TAG, "UserBook progress updated successfully")
                } else {
                    Log.w(TAG, "UserBook progress update failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UserBook progress: ${e.message}", e)
            }
        }
    }

    fun updateUserBookReview(userBook: UserBook, review: String?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating UserBook review: ${userBook.id}")
                val updated = userBook.copy(
                    review = review?.takeIf { it.isNotBlank() },
                    updatedAt = Date()
                )
                val success = userBookRepository.updateUserBook(updated)
                if (success) {
                    Log.d(TAG, "UserBook review updated successfully")
                } else {
                    Log.w(TAG, "UserBook review update failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UserBook review: ${e.message}", e)
            }
        }
    }

    fun toggleUserBookFavorite(userBook: UserBook) {
        viewModelScope.launch {
            try {
                val updated = userBook.copy(
                    favorite = !userBook.favorite,
                    updatedAt = Date()
                )
                val success = userBookRepository.updateUserBook(updated)
                if (success) {
                    Log.d(TAG, "UserBook favorite toggled successfully: ${updated.favorite}")
                } else {
                    Log.w(TAG, "UserBook favorite toggle failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling UserBook favorite: ${e.message}", e)
            }
        }
    }

    fun deleteUserBook(userBook: UserBook) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting UserBook: ${userBook.id}")
                val success = userBookRepository.deleteUserBook(userBook)
                if (success) {
                    Log.d(TAG, "UserBook deleted successfully")
                } else {
                    Log.w(TAG, "UserBook deletion failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting UserBook: ${e.message}", e)
            }
        }
    }

    // UserBook search and filter methods
    fun getUserBooksByStatus(status: UserBookReadingStatus): Flow<List<UserBook>> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            userBookRepository.getUserBooksByStatus(currentUser.id, status)
        } else {
            flowOf(emptyList())
        }
    }

    fun getUserBooksByWishlistStatus(status: UserBookWishlistStatus): Flow<List<UserBook>> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            userBookRepository.getUserBooksByWishlistStatus(currentUser.id, status)
        } else {
            flowOf(emptyList())
        }
    }

    fun searchUserBooks(query: String): Flow<List<UserBook>> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            userBookRepository.searchUserBooks(currentUser.id, query)
        } else {
            flowOf(emptyList())
        }
    }

    fun getFavoriteUserBooks(): Flow<List<UserBook>> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            userBookRepository.getFavoriteUserBooks(currentUser.id)
        } else {
            flowOf(emptyList())
        }
    }

    fun getCurrentlyReadingBooks(): Flow<List<UserBook>> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            userBookRepository.getCurrentlyReading(currentUser.id)
        } else {
            flowOf(emptyList())
        }
    }

    // UserBook sync methods
    fun syncUserBooksFromServer() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting UserBook sync from server")
                val result = userBookRepository.syncFromServer()
                when (result) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "UserBook sync from server completed successfully")
                    }
                    is SyncResult.Error -> {
                        Log.e(TAG, "UserBook sync from server failed: ${result.message}")
                    }
                    is SyncResult.Partial -> {
                        Log.w(TAG, "UserBook sync from server partial: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing UserBooks from server: ${e.message}", e)
            }
        }
    }

    fun syncUserBooksToServer() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting UserBook sync to server")
                val result = userBookRepository.syncToServer()
                when (result) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "UserBook sync to server completed successfully")
                    }
                    is SyncResult.Error -> {
                        Log.e(TAG, "UserBook sync to server failed: ${result.message}")
                    }
                    is SyncResult.Partial -> {
                        Log.w(TAG, "UserBook sync to server partial: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing UserBooks to server: ${e.message}", e)
            }
        }
    }

    fun fullUserBookSync() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting full UserBook sync")
                val result = userBookRepository.fullSync()
                when (result) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "Full UserBook sync completed successfully")
                    }
                    is SyncResult.Error -> {
                        Log.e(TAG, "Full UserBook sync failed: ${result.message}")
                    }
                    is SyncResult.Partial -> {
                        Log.w(TAG, "Full UserBook sync partial: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in full UserBook sync: ${e.message}", e)
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
                Log.d(TAG, "Deleting book: ${book.title} (ID: ${book.id})")

                // Perform the deletion
                val success = repository.deleteBook(book)

                if (success) {
                    Log.d(TAG, "Book deleted successfully from server and local DB: ${book.title}")
                } else {
                    Log.w(TAG, "Book deleted locally only due to network issues: ${book.title}")
                }

                // The Flow from repository.getAllBooks() should automatically update the UI
                // Room's @Delete operation automatically triggers Flow updates

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting book '${book.title}': ${e.message}", e)
                // Even if there's an error, the local deletion might have succeeded
                // Room will handle the Flow update automatically
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



    // Google Books search functionality
    val googleSearchResults: StateFlow<List<SearchResult>> = searchVm.googleSearchResults
    val isSearchingGoogle: StateFlow<Boolean> = searchVm.isSearchingGoogle
    val searchError: StateFlow<String?> = searchVm.searchError
    val combinedSearchResults = searchVm.combinedSearchResults



}