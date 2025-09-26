package com.example.mybookhoard.api.books

import android.content.Context

/**
 * Provider for API services to maintain clean architecture
 * Path: app/src/main/java/com/example/mybookhoard/api/books/ApiServiceProvider.kt
 */
object ApiServiceProvider {

    private var _booksApiService: BooksApiService? = null
    private var _userBooksApiService: UserBooksApiService? = null

    fun getBooksApiService(context: Context): BooksApiService {
        return _booksApiService ?: synchronized(this) {
            _booksApiService ?: BooksApiService(context).also { _booksApiService = it }
        }
    }

    fun getUserBooksApiService(context: Context): UserBooksApiService {
        return _userBooksApiService ?: synchronized(this) {
            _userBooksApiService ?: UserBooksApiService(context).also { _userBooksApiService = it }
        }
    }

    /**
     * Clear all service instances (useful for testing or logout)
     */
    fun clearServices() {
        _booksApiService = null
        _userBooksApiService = null
    }
}