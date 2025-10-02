package com.example.mybookhoard.utils

import com.example.mybookhoard.data.entities.BookWithUserDataExtended

/**
 * Utility object for sorting books by saga
 */
object BookSorting {

    /**
     * Sorts books alphabetically by title, but when a book belongs to a saga,
     * all books from that saga appear together immediately after the first book.
     *
     * Example with books: A (no saga), B (saga X #1), D (saga X #2), C (no saga)
     * Result: A, B, D, C
     *
     * Sort order:
     * 1. Start with all books sorted alphabetically by title
     * 2. When a book has a saga, insert all other books from that saga right after it
     * 3. Skip books from sagas already processed
     */
    fun sortBySaga(books: List<BookWithUserDataExtended>): List<BookWithUserDataExtended> {
        val result = mutableListOf<BookWithUserDataExtended>()
        val processedSagaIds = mutableSetOf<Long>()

        // Sort all books alphabetically by title
        val sortedByTitle = books.sortedBy { it.book.title.lowercase() }

        for (book in sortedByTitle) {
            val sagaId = book.book.sagaId

            if (sagaId == null) {
                // Book without saga - add it directly
                result.add(book)
            } else if (!processedSagaIds.contains(sagaId)) {
                // First book of this saga - add all books from this saga in order
                val sagaBooks = books
                    .filter { it.book.sagaId == sagaId }
                    .sortedWith(
                        compareBy(
                            // Sort by saga number
                            { it.book.sagaNumber ?: Int.MAX_VALUE },
                            // Then by title as tiebreaker
                            { it.book.title.lowercase() }
                        )
                    )

                result.addAll(sagaBooks)
                processedSagaIds.add(sagaId)
            }
            // If saga already processed, skip this book (it was already added)
        }

        return result
    }
}