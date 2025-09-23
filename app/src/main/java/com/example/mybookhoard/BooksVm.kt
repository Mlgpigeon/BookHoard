package com.example.mybookhoard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookhoard.api.*
import com.example.mybookhoard.data.*
import com.example.mybookhoard.repository.BookRepository
import com.example.mybookhoard.viewmodels.AuthVm
import com.example.mybookhoard.viewmodels.SearchVm
import com.example.mybookhoard.viewmodels.SyncVm
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BooksVm(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "BooksVm"
    }

    private val repository = BookRepository(app)

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
                Log.d(TAG, "Deleting book: ${book.title}")
                val success = repository.deleteBook(book)
                if (success) {
                    Log.d(TAG, "Book deleted successfully: ${book.title}")
                } else {
                    Log.w(TAG, "Book deleted locally only: ${book.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting book '${book.title}': ${e.message}", e)
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