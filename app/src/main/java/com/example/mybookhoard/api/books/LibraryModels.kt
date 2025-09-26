package com.example.mybookhoard.api.books

import com.example.mybookhoard.data.entities.Book
import com.example.mybookhoard.data.entities.UserBook

/**
 * Result of optimized library data fetch
 */
sealed class LibraryResult {
    data class Success(val items: List<LibraryItem>, val totalCount: Int) : LibraryResult()
    data class Error(val message: String) : LibraryResult()
}

/**
 * Combined library item with book, user book data and author info
 */
data class LibraryItem(
    val book: Book,
    val userBook: UserBook,
    val authorName: String? = null,
    val sagaName: String? = null
) {
    /**
     * Convert to BookWithUserDataExtended for UI compatibility
     */
    fun toBookWithUserDataExtended(): com.example.mybookhoard.data.entities.BookWithUserDataExtended {
        return com.example.mybookhoard.data.entities.BookWithUserDataExtended(
            book = book,
            userBook = userBook,
            authorName = authorName,
            sagaName = sagaName,
            sourceLabel = when (book.source.name) {
                "GOOGLE_BOOKS_API" -> "from Google Books"
                "OPENLIBRARY_API" -> "from Open Library"
                else -> null
            }
        )
    }

    /**
     * Convert to BookWithUserData for basic UI
     */
    fun toBookWithUserData(): com.example.mybookhoard.data.entities.BookWithUserData {
        return com.example.mybookhoard.data.entities.BookWithUserData(
            book = book,
            userBook = userBook
        )
    }
}